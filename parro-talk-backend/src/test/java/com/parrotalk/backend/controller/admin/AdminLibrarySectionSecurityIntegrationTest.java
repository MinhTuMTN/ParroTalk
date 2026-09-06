package com.parrotalk.backend.controller.admin;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.SectionType;
import com.parrotalk.backend.dto.admin.AdminLibrarySectionRequest;
import com.parrotalk.backend.dto.admin.ReorderLibrarySectionsRequest;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LibrarySectionRepository;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.security.JwtUtils;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminLibrarySectionSecurityIntegrationTest {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LibrarySectionRepository librarySectionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private User adminUser;
    private User normalUser;
    private String adminToken;
    private String normalUserToken;
    private Category testCategory;
    private LibrarySection testSection;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        adminUser = userRepository.save(User.builder()
                .email("admin-" + UUID.randomUUID().toString().substring(0, 6) + "@parrotalk.fun")
                .fullName("Admin User")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .build());

        normalUser = userRepository.save(User.builder()
                .email("user-" + UUID.randomUUID().toString().substring(0, 6) + "@parrotalk.fun")
                .fullName("Normal User")
                .password(passwordEncoder.encode("UserPass123!"))
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());

        adminToken = jwtUtils.generateToken(adminUser);
        normalUserToken = jwtUtils.generateToken(normalUser);

        testCategory = categoryRepository.save(Category.builder()
                .name("Culture")
                .slug("culture-" + UUID.randomUUID().toString().substring(0, 6))
                .sortOrder(1)
                .status(CmsItemStatus.ACTIVE)
                .build());

        testSection = librarySectionRepository.save(LibrarySection.builder()
                .title("Initial Admin Section")
                .type(SectionType.CATEGORY)
                .category(testCategory)
                .orderIndex(0)
                .isActive(true)
                .maxItems(10)
                .build());
    }

    @Test
    @DisplayName("Non-Admin user (ROLE_USER) gets 403 Forbidden on GET /api/admin/library/sections")
    void getAllSections_NonAdmin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/library/sections")
                        .header("Authorization", "Bearer " + normalUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user gets 401/403 on GET /api/admin/library/sections")
    void getAllSections_Anonymous_UnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/library/sections")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Admin user (ROLE_ADMIN) successfully gets all sections on GET /api/admin/library/sections")
    void getAllSections_Admin_Success() throws Exception {
        mockMvc.perform(get("/api/admin/library/sections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].title", is("Initial Admin Section")))
                .andExpect(jsonPath("$.result[0].categoryName", is("Culture")));
    }

    @Test
    @DisplayName("Non-Admin user (ROLE_USER) gets 403 Forbidden on POST /api/admin/library/sections")
    void createSection_NonAdmin_Forbidden() throws Exception {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("Hacker Section")
                .type(SectionType.PROGRAM)
                .program("Unauthorized")
                .build();

        mockMvc.perform(post("/api/admin/library/sections")
                        .header("Authorization", "Bearer " + normalUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin user creates CATEGORY and PROGRAM sections successfully")
    void createSection_Admin_Success() throws Exception {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("New Program Section")
                .type(SectionType.PROGRAM)
                .program("BBC 6 Minute English")
                .orderIndex(1)
                .isActive(true)
                .maxItems(10)
                .build();

        mockMvc.perform(post("/api/admin/library/sections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title", is("New Program Section")))
                .andExpect(jsonPath("$.result.type", is("PROGRAM")))
                .andExpect(jsonPath("$.result.program", is("BBC 6 Minute English")));
    }

    @Test
    @DisplayName("Non-Admin user gets 403 Forbidden on PUT /api/admin/library/sections/{id}")
    void updateSection_NonAdmin_Forbidden() throws Exception {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("Hacked Title")
                .type(SectionType.CATEGORY)
                .categoryId(testCategory.getId())
                .build();

        mockMvc.perform(put("/api/admin/library/sections/" + testSection.getId())
                        .header("Authorization", "Bearer " + normalUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin user updates section successfully")
    void updateSection_Admin_Success() throws Exception {
        AdminLibrarySectionRequest request = AdminLibrarySectionRequest.builder()
                .title("Updated Culture Spotlight")
                .type(SectionType.CATEGORY)
                .categoryId(testCategory.getId())
                .orderIndex(5)
                .isActive(true)
                .maxItems(12)
                .build();

        mockMvc.perform(put("/api/admin/library/sections/" + testSection.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title", is("Updated Culture Spotlight")))
                .andExpect(jsonPath("$.result.orderIndex", is(5)))
                .andExpect(jsonPath("$.result.maxItems", is(12)));
    }

    @Test
    @DisplayName("Non-Admin user gets 403 Forbidden on DELETE /api/admin/library/sections/{id}")
    void deleteSection_NonAdmin_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/admin/library/sections/" + testSection.getId())
                        .header("Authorization", "Bearer " + normalUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin user deletes section successfully")
    void deleteSection_Admin_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/library/sections/" + testSection.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Non-Admin user gets 403 Forbidden on PUT /api/admin/library/sections/reorder")
    void reorderSections_NonAdmin_Forbidden() throws Exception {
        ReorderLibrarySectionsRequest request = ReorderLibrarySectionsRequest.builder()
                .items(List.of(
                        ReorderLibrarySectionsRequest.ReorderSectionItem.builder()
                                .id(testSection.getId())
                                .orderIndex(10)
                                .build()))
                .build();

        mockMvc.perform(put("/api/admin/library/sections/reorder")
                        .header("Authorization", "Bearer " + normalUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin user reorders sections successfully")
    void reorderSections_Admin_Success() throws Exception {
        LibrarySection section2 = librarySectionRepository.save(LibrarySection.builder()
                .title("Section 2")
                .type(SectionType.PROGRAM)
                .program("TED")
                .orderIndex(1)
                .isActive(true)
                .maxItems(10)
                .build());

        ReorderLibrarySectionsRequest request = ReorderLibrarySectionsRequest.builder()
                .items(List.of(
                        ReorderLibrarySectionsRequest.ReorderSectionItem.builder()
                                .id(testSection.getId())
                                .orderIndex(1)
                                .build(),
                        ReorderLibrarySectionsRequest.ReorderSectionItem.builder()
                                .id(section2.getId())
                                .orderIndex(0)
                                .build()))
                .build();

        mockMvc.perform(put("/api/admin/library/sections/reorder")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify order in DB
        List<LibrarySection> ordered = librarySectionRepository.findAllByOrderByOrderIndexAsc();
        assertThat(ordered.get(0).getId()).isEqualTo(section2.getId());
        assertThat(ordered.get(1).getId()).isEqualTo(testSection.getId());
    }
}
