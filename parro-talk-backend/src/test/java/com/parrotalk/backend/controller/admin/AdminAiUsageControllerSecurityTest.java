package com.parrotalk.backend.controller.admin;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.repository.AiUsageStatsRepository;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.security.JwtUtils;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminAiUsageControllerSecurityTest {

    @MockitoBean
    private ResendClient resendClient;
    @MockitoBean
    private ConnectionFactory connectionFactory;
    @MockitoBean
    private StringRedisTemplate redisTemplate;
    /** Native PostgreSQL queries cannot run on H2; the security/validation layers are what's under test. */
    @MockitoBean
    private AiUsageStatsRepository statsRepository;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        adminToken = jwtUtils.generateToken(save(Role.ADMIN));
        userToken = jwtUtils.generateToken(save(Role.USER));
    }

    private User save(Role role) {
        return userRepository.save(User.builder()
                .email(role + "-" + UUID.randomUUID().toString().substring(0, 6) + "@parrotalk.fun")
                .fullName(role.name())
                .password(passwordEncoder.encode("Passw0rd!123"))
                .role(role).enabled(true).emailVerified(true).build());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/summary"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/admin/ai-usage/summary"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getStatus() == 401 || result.getResponse().getStatus() == 403));
    }

    @Test
    void nonAdminForbidden() throws Exception {
        for (String p : new String[] { "summary", "timeseries", "models", "users", "features" }) {
            mockMvc.perform(get("/api/admin/ai-usage/" + p).header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminGetsOkEverywhere() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/summary").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalRequests").value(0));
        mockMvc.perform(get("/api/admin/ai-usage/timeseries").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(8)));
        for (String p : new String[] { "models", "features" }) {
            mockMvc.perform(get("/api/admin/ai-usage/" + p).header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result", hasSize(0)));
        }
        mockMvc.perform(get("/api/admin/ai-usage/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(0));
    }

    @Test
    void invalidParamsReturn400() throws Exception {
        String auth = "Bearer " + adminToken;
        mockMvc.perform(get("/api/admin/ai-usage/summary").param("from", "2026-11-10").param("to", "2026-11-01")
                .header("Authorization", auth)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/admin/ai-usage/summary").param("from", "2026-01-01").param("to", "2026-11-01")
                .header("Authorization", auth)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/ai-usage/users").param("size", "101")
                .header("Authorization", auth)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/ai-usage/summary").param("userId", "nope")
                .header("Authorization", auth)).andExpect(status().isBadRequest());
    }
}
