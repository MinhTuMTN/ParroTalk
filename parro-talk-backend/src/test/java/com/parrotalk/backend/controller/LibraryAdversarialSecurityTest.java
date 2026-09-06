package com.parrotalk.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.ParroTalkBackendApplication;
import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.SectionType;
import com.parrotalk.backend.constant.SourceType;
import com.parrotalk.backend.dto.admin.AdminLibrarySectionRequest;
import com.parrotalk.backend.dto.admin.ReorderLibrarySectionsRequest;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserLessonProgress;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.LibrarySectionRepository;
import com.parrotalk.backend.repository.UserLessonProgressRepository;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.security.JwtUtils;

/**
 * Adversarial and Stress Test Suite for Library UI & Backend API (Milestone 6).
 * 
 * Tests:
 * 1. Admin RBAC penetration (Unauthorized / Forbidden enforcement on all HTTP methods).
 * 2. Cross-user data isolation (User A vs User B vs Anonymous zero data leakage).
 * 3. Edge-case queries (SQL injection payloads, XSS payloads, Unicode, non-existent resources, invalid paging/sorts).
 * 4. Payload security & minimalism (Ensuring zero leakage of internal transcript/segment entities).
 */
@SpringBootTest(classes = ParroTalkBackendApplication.class)
@ActiveProfiles("test")
@Transactional
public class LibraryAdversarialSecurityTest {

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
    private LessonRepository lessonRepository;

    @Autowired
    private LibrarySectionRepository librarySectionRepository;

    @Autowired
    private UserLessonProgressRepository progressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private User adminUser;
    private User userA;
    private User userB;
    private String adminToken;
    private String userAToken;
    private String userBToken;
    private Category scienceCategory;
    private Category artCategory;
    private Lesson userAPrivateLesson;
    private Lesson userAProcessingLesson;
    private Lesson userBPrivateLesson;
    private Lesson publicDoneLesson;
    private Lesson publicProcessingLesson;
    private LibrarySection activeCategorySection;
    private LibrarySection activeProgramSection;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        // 1. Seed Users
        adminUser = userRepository.save(User.builder()
                .email("admin-adv-" + UUID.randomUUID() + "@parrotalk.fun")
                .fullName("Admin Adv")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .build());

        userA = userRepository.save(User.builder()
                .email("user-a-adv-" + UUID.randomUUID() + "@parrotalk.fun")
                .fullName("User A")
                .password(passwordEncoder.encode("UserPass123!"))
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());

        userB = userRepository.save(User.builder()
                .email("user-b-adv-" + UUID.randomUUID() + "@parrotalk.fun")
                .fullName("User B")
                .password(passwordEncoder.encode("UserPass123!"))
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());

        adminToken = jwtUtils.generateToken(adminUser);
        userAToken = jwtUtils.generateToken(userA);
        userBToken = jwtUtils.generateToken(userB);

        // 2. Seed Categories
        scienceCategory = categoryRepository.save(Category.builder()
                .name("Science & Discovery")
                .slug("science-discovery-" + UUID.randomUUID().toString().substring(0, 6))
                .sortOrder(1)
                .status(CmsItemStatus.ACTIVE)
                .build());

        artCategory = categoryRepository.save(Category.builder()
                .name("Art & Design")
                .slug("art-design-" + UUID.randomUUID().toString().substring(0, 6))
                .sortOrder(2)
                .status(CmsItemStatus.ACTIVE)
                .build());

        // 3. Seed Lessons
        // Public Done Lesson (Available to everyone)
        publicDoneLesson = lessonRepository.save(Lesson.builder()
                .title("Quantum Computing Explained")
                .fileUrl("https://example.com/quantum.mp3")
                .mediaType(com.parrotalk.backend.constant.MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .program("BBC 6 Minute")
                .source("BBC")
                .duration(360)
                .categories(Set.of(scienceCategory))
                .build());

        // Public Processing Lesson (Should NOT be visible in public sections/search)
        publicProcessingLesson = lessonRepository.save(Lesson.builder()
                .title("Drafting Astrophysics")
                .fileUrl("https://example.com/astro.mp3")
                .mediaType(com.parrotalk.backend.constant.MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.PROCESSING)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .duration(200)
                .categories(Set.of(scienceCategory))
                .build());

        // User A Private Lesson (Hidden, Done, Owned by A)
        userAPrivateLesson = lessonRepository.save(Lesson.builder()
                .title("Secret Diary of User A")
                .fileUrl("https://example.com/user-a-secret.mp3")
                .mediaType(com.parrotalk.backend.constant.MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .ownerId(userA.getId())
                .duration(120)
                .categories(Set.of(scienceCategory))
                .build());

        // User A Processing Lesson (Hidden, Processing, Owned by A)
        userAProcessingLesson = lessonRepository.save(Lesson.builder()
                .title("User A Audio In Flight")
                .fileUrl("https://example.com/user-a-inflight.mp3")
                .mediaType(com.parrotalk.backend.constant.MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.PROCESSING)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .ownerId(userA.getId())
                .duration(90)
                .build());

        // User B Private Lesson (Hidden, Done, Owned by B)
        userBPrivateLesson = lessonRepository.save(Lesson.builder()
                .title("Confidential Notes of User B")
                .fileUrl("https://example.com/user-b-secret.mp3")
                .mediaType(com.parrotalk.backend.constant.MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .ownerId(userB.getId())
                .duration(300)
                .categories(Set.of(artCategory))
                .build());

        // 4. Progress setup
        // User A is halfway through publicDoneLesson
        progressRepository.save(UserLessonProgress.builder()
                .id(new UserLessonProgress.UserLessonProgressId(userA.getId(), publicDoneLesson.getId()))
                .user(userA)
                .lesson(publicDoneLesson)
                .lastProgress(0.65)
                .build());

        // 5. Setup Library Sections
        librarySectionRepository.save(LibrarySection.builder()
                .title("Continue Learning")
                .type(SectionType.IN_PROGRESS)
                .orderIndex(0)
                .isActive(true)
                .maxItems(10)
                .build());

        librarySectionRepository.save(LibrarySection.builder()
                .title("Processing Audio")
                .type(SectionType.USER_PROCESSING)
                .orderIndex(1)
                .isActive(true)
                .maxItems(10)
                .build());

        librarySectionRepository.save(LibrarySection.builder()
                .title("Uploaded Lessons")
                .type(SectionType.USER_UPLOADED)
                .orderIndex(2)
                .isActive(true)
                .maxItems(10)
                .build());

        activeCategorySection = librarySectionRepository.save(LibrarySection.builder()
                .title("Science Discovery Shelf")
                .type(SectionType.CATEGORY)
                .category(scienceCategory)
                .orderIndex(3)
                .isActive(true)
                .maxItems(10)
                .build());

        activeProgramSection = librarySectionRepository.save(LibrarySection.builder()
                .title("BBC 6 Minute Series")
                .type(SectionType.PROGRAM)
                .program("BBC 6 Minute")
                .orderIndex(4)
                .isActive(true)
                .maxItems(10)
                .build());
    }

    // =========================================================================
    // 1. ADMIN RBAC PENETRATION SUITE
    // =========================================================================

    @Test
    @DisplayName("Attack 1.1: Anonymous request to GET /api/admin/library/sections is blocked (401/403)")
    void testAnonymousCannotReadAdminSections() throws Exception {
        mockMvc.perform(get("/api/admin/library/sections")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Attack 1.2: Regular User (ROLE_USER) to GET /api/admin/library/sections is blocked (403 Forbidden)")
    void testRegularUserCannotReadAdminSections() throws Exception {
        mockMvc.perform(get("/api/admin/library/sections")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Attack 1.3: Regular User attempting POST /api/admin/library/sections is blocked (403 Forbidden)")
    void testRegularUserCannotCreateAdminSection() throws Exception {
        AdminLibrarySectionRequest payload = AdminLibrarySectionRequest.builder()
                .title("Malicious Section")
                .type(SectionType.PROGRAM)
                .program("Malicious Program")
                .build();

        mockMvc.perform(post("/api/admin/library/sections")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Attack 1.4: Regular User attempting PUT /api/admin/library/sections/{id} is blocked (403 Forbidden)")
    void testRegularUserCannotUpdateAdminSection() throws Exception {
        AdminLibrarySectionRequest payload = AdminLibrarySectionRequest.builder()
                .title("Defaced Section")
                .type(SectionType.CATEGORY)
                .categoryId(scienceCategory.getId())
                .build();

        mockMvc.perform(put("/api/admin/library/sections/" + activeCategorySection.getId())
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Attack 1.5: Regular User attempting DELETE /api/admin/library/sections/{id} is blocked (403 Forbidden)")
    void testRegularUserCannotDeleteAdminSection() throws Exception {
        mockMvc.perform(delete("/api/admin/library/sections/" + activeCategorySection.getId())
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Attack 1.6: Regular User attempting PUT /api/admin/library/sections/reorder is blocked (403 Forbidden)")
    void testRegularUserCannotReorderAdminSections() throws Exception {
        ReorderLibrarySectionsRequest request = ReorderLibrarySectionsRequest.builder()
                .items(List.of(
                        ReorderLibrarySectionsRequest.ReorderSectionItem.builder()
                                .id(activeCategorySection.getId())
                                .orderIndex(99)
                                .build()))
                .build();

        mockMvc.perform(put("/api/admin/library/sections/reorder")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Attack 1.7: Invalid Admin payload triggers 400 Bad Request rather than 500 error")
    void testInvalidAdminPayloadValidation() throws Exception {
        // Missing title and type
        AdminLibrarySectionRequest emptyPayload = AdminLibrarySectionRequest.builder().build();

        mockMvc.perform(post("/api/admin/library/sections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyPayload)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 2. CROSS-USER DATA ISOLATION & ZERO-LEAKAGE SUITE
    // =========================================================================

    @Test
    @DisplayName("Attack 2.1: User B querying /api/library must NEVER see User A's private, processing, or uploaded lessons")
    void testUserBCannotSeeUserAPrivateOrProcessingLessonsInLibrary() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/library")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode sections = root.get("result");

        assertThat(sections.isArray()).isTrue();

        for (JsonNode section : sections) {
            JsonNode items = section.get("items");
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    String idStr = item.get("id").asText();
                    String titleStr = item.get("title").asText();

                    // Assert User A's private lessons are NEVER present
                    assertThat(idStr)
                            .isNotEqualTo(userAPrivateLesson.getId().toString())
                            .isNotEqualTo(userAProcessingLesson.getId().toString())
                            .isNotEqualTo(publicProcessingLesson.getId().toString());

                    assertThat(titleStr)
                            .isNotEqualTo("Secret Diary of User A")
                            .isNotEqualTo("User A Audio In Flight")
                            .isNotEqualTo("Drafting Astrophysics");
                }
            }
        }
    }

    @Test
    @DisplayName("Attack 2.2: Anonymous user querying /api/library receives zero personal sections and zero private items")
    void testAnonymousReceivesZeroPersonalSections() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/library")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode sections = root.get("result");

        for (JsonNode section : sections) {
            String type = section.get("type").asText();
            assertThat(type).isNotIn("IN_PROGRESS", "USER_PROCESSING", "USER_UPLOADED");

            JsonNode items = section.get("items");
            for (JsonNode item : items) {
                String idStr = item.get("id").asText();
                assertThat(idStr)
                        .isNotEqualTo(userAPrivateLesson.getId().toString())
                        .isNotEqualTo(userBPrivateLesson.getId().toString())
                        .isNotEqualTo(userAProcessingLesson.getId().toString())
                        .isNotEqualTo(publicProcessingLesson.getId().toString());
            }
        }
    }

    @Test
    @DisplayName("Attack 2.3: Searching for User A's private lesson title as User B returns 0 items (Zero Leakage)")
    void testSearchZeroLeakageForOtherUserPrivateLesson() throws Exception {
        mockMvc.perform(get("/api/library/search")
                        .param("q", "Secret Diary")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(0)))
                .andExpect(jsonPath("$.result.content").isEmpty());
    }

    @Test
    @DisplayName("Attack 2.4: Searching for User A's private lesson title as Anonymous returns 0 items")
    void testSearchZeroLeakageAnonymous() throws Exception {
        mockMvc.perform(get("/api/library/search")
                        .param("q", "Secret Diary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(0)))
                .andExpect(jsonPath("$.result.content").isEmpty());
    }

    @Test
    @DisplayName("Attack 2.5: Category lesson listing never leaks private or processing lessons")
    void testCategoryLessonsExcludesPrivateAndProcessing() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/library/categories/" + scienceCategory.getSlug() + "/lessons")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(1))) // Only publicDoneLesson
                .andExpect(jsonPath("$.result.content[0].id", is(publicDoneLesson.getId().toString())))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).doesNotContain("Secret Diary of User A");
        assertThat(json).doesNotContain("Drafting Astrophysics");
    }

    // =========================================================================
    // 3. EDGE-CASE QUERIES & FUZZING
    // =========================================================================

    @Test
    @DisplayName("Edge 3.1: Non-existent category slug returns 404 NOT FOUND gracefully")
    void testNonExistentCategorySlugReturns404() throws Exception {
        mockMvc.perform(get("/api/library/categories/non-existent-random-category-slug/lessons")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Edge 3.2: SQL Injection payload in category slug returns 404 NOT FOUND (No SQL syntax error / 500)")
    void testSqlInjectionInCategorySlug() throws Exception {
        String sqliSlug = "' OR 1=1 --";
        mockMvc.perform(get("/api/library/categories/{slug}/lessons", sqliSlug)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Edge 3.3: SQL Injection payload in search query returns 200 OK with empty result (No SQL leak)")
    void testSqlInjectionInSearchQuery() throws Exception {
        String sqliQuery = "' UNION SELECT * FROM users --";
        mockMvc.perform(get("/api/library/search")
                        .param("q", sqliQuery)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(0)))
                .andExpect(jsonPath("$.result.content").isEmpty());
    }

    @Test
    @DisplayName("Edge 3.4: Special characters and wildcards in search query handled gracefully")
    void testSpecialWildcardsInSearch() throws Exception {
        // Wildcard search "%", "_", "\\", quotes
        mockMvc.perform(get("/api/library/search")
                        .param("q", "%%%%%")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/library/search")
                        .param("q", "___")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/library/search")
                        .param("q", "<script>alert('xss')</script>")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(0)));
    }

    @Test
    @DisplayName("Edge 3.5: Negative page number and extreme page size handled gracefully")
    void testPagingEdgeCases() throws Exception {
        // Negative page is coerced to 0
        mockMvc.perform(get("/api/library/search")
                        .param("q", "Quantum")
                        .param("page", "-5")
                        .param("size", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.page", is(0)))
                .andExpect(jsonPath("$.result.totalElements", is(1)));
    }

    @Test
    @DisplayName("Edge 3.6: Unrecognized sort parameter falls back gracefully to default sort (latest)")
    void testUnknownSortParameterFallback() throws Exception {
        mockMvc.perform(get("/api/library/search")
                        .param("q", "Quantum")
                        .param("sort", "INVALID_SORT_KEY; DROP TABLE lessons;")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements", is(1)));
    }

    // =========================================================================
    // 4. PAYLOAD SECURITY & MINIMAL DTO VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("Payload 4.1: /api/library response items contain ONLY LessonWithProgressDTO scalar fields")
    void testLibrarySectionItemsMinimalPayload() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/library")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode sections = root.get("result");

        for (JsonNode section : sections) {
            JsonNode items = section.get("items");
            for (JsonNode item : items) {
                // Allowed scalar fields: id, title, progress, thumbnailUrl, url, duration
                assertThat(item.has("id")).isTrue();
                assertThat(item.has("title")).isTrue();
                assertThat(item.has("progress")).isTrue();
                assertThat(item.has("thumbnailUrl")).isTrue();
                assertThat(item.has("url")).isTrue();
                assertThat(item.has("duration")).isTrue();

                // Prohibited internal fields
                assertThat(item.has("segments")).isFalse();
                assertThat(item.has("transcript")).isFalse();
                assertThat(item.has("rawTranscript")).isFalse();
                assertThat(item.has("vocabulary")).isFalse();
                assertThat(item.has("ownerId")).isFalse();
                assertThat(item.has("user")).isFalse();
            }
        }
    }

    @Test
    @DisplayName("Payload 4.2: /api/library/search response content contains ONLY LessonWithProgressDTO fields")
    void testSearchResponseMinimalPayload() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/library/search")
                        .param("q", "Quantum")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode content = root.get("result").get("content");

        for (JsonNode item : content) {
            assertThat(item.has("id")).isTrue();
            assertThat(item.has("title")).isTrue();
            assertThat(item.has("progress")).isTrue();
            assertThat(item.has("thumbnailUrl")).isTrue();
            assertThat(item.has("url")).isTrue();
            assertThat(item.has("duration")).isTrue();

            assertThat(item.has("segments")).isFalse();
            assertThat(item.has("transcript")).isFalse();
            assertThat(item.has("rawTranscript")).isFalse();
            assertThat(item.has("ownerId")).isFalse();
        }
    }
}
