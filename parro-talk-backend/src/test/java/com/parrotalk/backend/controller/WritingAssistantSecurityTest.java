package com.parrotalk.backend.controller;

import com.parrotalk.backend.ParroTalkBackendApplication;
import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.service.WritingTtsService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ParroTalkBackendApplication.class)
@ActiveProfiles("test")
class WritingAssistantSecurityTest {

    @TempDir
    static Path tempTtsCacheDir;

    @DynamicPropertySource
    static void registerTtsProperties(DynamicPropertyRegistry registry) {
        registry.add("app.tts.cache-dir", () -> tempTtsCacheDir.toAbsolutePath().toString());
    }

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private WritingTtsService writingTtsService;

    @Value("${application.security.jwt.secret-key}")
    private String jwtSecretKey;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /api/writing/tts is publicly accessible without auth and reaches WritingTtsService validation")
    void testGetTts_AnonymousAllowedAndValidated() throws Exception {
        assertNotNull(writingTtsService);

        mockMvc.perform(get("/api/writing/tts")
                        .param("text", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/writing/tts still succeeds with malformed or expired Bearer tokens while protected endpoints reject them")
    void testGetTts_WithMalformedOrExpiredBearerToken_RemainsPermitted() throws Exception {
        byte[] cachedBytes = "ID3-security-test-audio".getBytes(StandardCharsets.UTF_8);
        String cacheFileName = writingTtsService.computeCacheFileName("hello", "US");
        Files.write(tempTtsCacheDir.resolve(cacheFileName), cachedBytes);

        String malformedHeader = "Bearer invalid.jwt.token";
        String expiredToken = Jwts.builder()
                .setSubject("expired@parrotalk.fun")
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
        String expiredHeader = "Bearer " + expiredToken;

        mockMvc.perform(get("/api/writing/tts")
                        .param("text", "hello")
                        .param("accent", "US")
                        .header("Authorization", malformedHeader))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(cachedBytes));

        mockMvc.perform(get("/api/writing/tts")
                        .param("text", "hello")
                        .param("accent", "US")
                        .header("Authorization", expiredHeader))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(cachedBytes));

        mockMvc.perform(get("/api/writing/sessions")
                        .header("Authorization", malformedHeader))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/writing/sessions")
                        .header("Authorization", expiredHeader))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/writing/tts and POST /api/writing/analyze remain protected for anonymous requests")
    void testOtherWritingMethods_AnonymousBlocked() throws Exception {
        mockMvc.perform(post("/api/writing/tts")
                        .param("text", "hello"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/writing/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
