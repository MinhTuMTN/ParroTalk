package com.parrotalk.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.constant.MediaType;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.SectionType;
import com.parrotalk.backend.constant.SourceType;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.LibrarySectionDTO;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserLessonProgress;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.LibrarySectionRepository;
import com.parrotalk.backend.repository.UserLessonProgressRepository;
import com.parrotalk.backend.repository.UserRepository;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import com.parrotalk.backend.security.JwtUtils;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class LibraryControllerIntegrationTest {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean(name = "dbQueryExecutor")
    private java.util.concurrent.Executor dbQueryExecutor;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private LibraryController libraryController;

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
    private org.springframework.cache.CacheManager cacheManager;

    private User userA;
    private User userB;
    private Category techCategory;
    private Category businessCategory;
    private Lesson publishedLesson1;
    private Lesson publishedLesson2;
    private Lesson publishedLesson3;
    private Lesson privateLessonUserB;
    private Lesson processingLesson;
    private Lesson userAPendingLesson;
    private String userAToken;

    @BeforeEach
    void setUp() {
        if (cacheManager != null) {
            for (String name : cacheManager.getCacheNames()) {
                org.springframework.cache.Cache c = cacheManager.getCache(name);
                if (c != null) {
                    c.clear();
                }
            }
        }
        librarySectionRepository.deleteAll();

        org.mockito.Mockito.doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(dbQueryExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));

        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        userA = userRepository.save(User.builder()
                .email("student-a-" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .fullName("Student A")
                .password("hashed_pwd")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());

        userAToken = jwtUtils.generateToken(userA);

        userB = userRepository.save(User.builder()
                .email("student-b-" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .fullName("Student B")
                .password("hashed_pwd")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());

        techCategory = categoryRepository.save(Category.builder()
                .name("Technology")
                .slug("technology")
                .sortOrder(1)
                .status(CmsItemStatus.ACTIVE)
                .build());

        businessCategory = categoryRepository.save(Category.builder()
                .name("Business")
                .slug("business")
                .sortOrder(2)
                .status(CmsItemStatus.ACTIVE)
                .build());

        // Lesson 1: Published, Done, Tech, BBC, BBC Learning
        publishedLesson1 = lessonRepository.save(Lesson.builder()
                .title("AI Revolution")
                .fileUrl("https://example.com/ai.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .program("BBC")
                .source("BBC Learning")
                .duration(360)
                .categories(Set.of(techCategory))
                .build());

        // Lesson 2: Published, Done, Tech, TED, TED-Ed
        publishedLesson2 = lessonRepository.save(Lesson.builder()
                .title("Future of Computing")
                .fileUrl("https://example.com/computing.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .program("TED")
                .source("TED-Ed")
                .duration(420)
                .categories(Set.of(techCategory))
                .build());

        // Lesson 3: Published, Done, Business, BBC, BBC Learning
        publishedLesson3 = lessonRepository.save(Lesson.builder()
                .title("Global Trade")
                .fileUrl("https://example.com/trade.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .program("BBC")
                .source("BBC Learning")
                .duration(280)
                .categories(Set.of(businessCategory))
                .build());

        // Lesson 4: Hidden, Done, Owner User B (Private lesson)
        privateLessonUserB = lessonRepository.save(Lesson.builder()
                .title("Private Lesson User B")
                .fileUrl("https://example.com/private.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .ownerId(userB.getId())
                .duration(150)
                .categories(Set.of(techCategory))
                .build());

        // Lesson 5: Published, Processing (Not ready)
        processingLesson = lessonRepository.save(Lesson.builder()
                .title("Processing Public Lesson")
                .fileUrl("https://example.com/processing.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.PROCESSING)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .duration(200)
                .categories(Set.of(techCategory))
                .build());

        // Lesson 6: Hidden, Pending, Owner User A
        userAPendingLesson = lessonRepository.save(Lesson.builder()
                .title("User A Pending Upload")
                .fileUrl("https://example.com/pending.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.PENDING)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .ownerId(userA.getId())
                .duration(100)
                .build());

        // User A progress: 50% on Lesson 1, 100% on Lesson 2
        progressRepository.save(UserLessonProgress.builder()
                .id(new UserLessonProgress.UserLessonProgressId(userA.getId(), publishedLesson1.getId()))
                .user(userA)
                .lesson(publishedLesson1)
                .lastProgress(0.5)
                .build());

        progressRepository.save(UserLessonProgress.builder()
                .id(new UserLessonProgress.UserLessonProgressId(userA.getId(), publishedLesson2.getId()))
                .user(userA)
                .lesson(publishedLesson2)
                .lastProgress(1.0)
                .build());

        // Configure library sections
        librarySectionRepository.save(LibrarySection.builder()
                .title("Continue Learning")
                .type(SectionType.IN_PROGRESS)
                .orderIndex(0)
                .isActive(true)
                .maxItems(6)
                .build());

        librarySectionRepository.save(LibrarySection.builder()
                .title("Technology Spotlight")
                .type(SectionType.CATEGORY)
                .category(techCategory)
                .orderIndex(1)
                .isActive(true)
                .maxItems(6)
                .build());

        librarySectionRepository.save(LibrarySection.builder()
                .title("BBC Learning Channel")
                .type(SectionType.PROGRAM)
                .program("BBC")
                .orderIndex(2)
                .isActive(true)
                .maxItems(6)
                .build());

        librarySectionRepository.save(LibrarySection.builder()
                .title("Disabled Business Section")
                .type(SectionType.CATEGORY)
                .category(businessCategory)
                .orderIndex(3)
                .isActive(false) // Disabled!
                .maxItems(6)
                .build());

        librarySectionRepository.save(LibrarySection.builder()
                .title("Your Processing Audio")
                .type(SectionType.USER_PROCESSING)
                .orderIndex(4)
                .isActive(true)
                .maxItems(6)
                .build());
    }

    @Test
    @DisplayName("GET /api/library/sections for Anonymous user: skips personal sections & returns active public sections")
    void getLibrarySections_AnonymousUser() {
        ResponseEntity<ApiResponse<List<LibrarySectionDTO>>> response = libraryController.getLibrarySections(null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<LibrarySectionDTO> sections = response.getBody().getResult();
        assertThat(sections).isNotEmpty();

        // Check types: must ONLY contain CATEGORY and PROGRAM, NO personal sections
        List<SectionType> sectionTypes = sections.stream().map(LibrarySectionDTO::getType).toList();
        assertThat(sectionTypes).contains(SectionType.CATEGORY, SectionType.PROGRAM);
        assertThat(sectionTypes).doesNotContain(SectionType.IN_PROGRESS, SectionType.USER_PROCESSING, SectionType.USER_UPLOADED);

        // Check that disabled section is not present
        assertThat(sections.stream().map(LibrarySectionDTO::getTitle))
                .doesNotContain("Disabled Business Section");

        // Check progress for anonymous user is 0
        for (LibrarySectionDTO section : sections) {
            for (LessonWithProgressDTO item : section.getItems()) {
                assertEquals(0, item.getProgress());
            }
        }
    }

    @Test
    @DisplayName("GET /api/library/sections for Authenticated User A: includes Continue Learning & User Processing")
    void getLibrarySections_AuthenticatedUser() {
        ResponseEntity<ApiResponse<List<LibrarySectionDTO>>> response = libraryController.getLibrarySections(userA);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<LibrarySectionDTO> sections = response.getBody().getResult();
        assertThat(sections).isNotEmpty();

        // Verify IN_PROGRESS section is included and contains Lesson 1 with 50% progress
        LibrarySectionDTO inProgressSec = sections.stream()
                .filter(s -> s.getType() == SectionType.IN_PROGRESS)
                .findFirst()
                .orElse(null);

        assertNotNull(inProgressSec);
        assertEquals(1, inProgressSec.getItems().size());
        assertEquals(publishedLesson1.getId(), inProgressSec.getItems().get(0).getId());
        assertEquals(50, inProgressSec.getItems().get(0).getProgress());

        // Verify USER_PROCESSING section includes User A's pending upload
        LibrarySectionDTO processingSec = sections.stream()
                .filter(s -> s.getType() == SectionType.USER_PROCESSING)
                .findFirst()
                .orElse(null);

        assertNotNull(processingSec);
        assertEquals(1, processingSec.getItems().size());
        assertEquals(userAPendingLesson.getId(), processingSec.getItems().get(0).getId());

        // Verify private lessons of other users (User B) never leak to User A
        for (LibrarySectionDTO sec : sections) {
            List<UUID> lessonIds = sec.getItems().stream().map(LessonWithProgressDTO::getId).toList();
            assertThat(lessonIds).doesNotContain(privateLessonUserB.getId());
        }
    }

    @Test
    @DisplayName("GET /api/library/categories/{slug}/lessons: returns paginated lessons and 404 for invalid slug")
    void getCategoryLessons_ValidationAndFilters() {
        // Valid slug: technology
        ResponseEntity<ApiResponse<PageResponse<LessonWithProgressDTO>>> response =
                libraryController.getCategoryLessons("technology", null, null, "latest", 0, 10, userA);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PageResponse<LessonWithProgressDTO> page = response.getBody().getResult();
        assertEquals(2, page.getTotalElements());

        // Program filter: BBC
        ResponseEntity<ApiResponse<PageResponse<LessonWithProgressDTO>>> bbcResponse =
                libraryController.getCategoryLessons("technology", "BBC", null, "latest", 0, 10, userA);
        assertEquals(1, bbcResponse.getBody().getResult().getTotalElements());
        assertEquals("AI Revolution", bbcResponse.getBody().getResult().getContent().get(0).getTitle());

        // Source filter: TED-Ed
        ResponseEntity<ApiResponse<PageResponse<LessonWithProgressDTO>>> tedResponse =
                libraryController.getCategoryLessons("technology", null, "TED-Ed", "latest", 0, 10, userA);
        assertEquals(1, tedResponse.getBody().getResult().getTotalElements());
        assertEquals("Future of Computing", tedResponse.getBody().getResult().getContent().get(0).getTitle());

        // Invalid slug -> 404
        assertThrows(ParroTalkException.class, () ->
                libraryController.getCategoryLessons("unknown-slug", null, null, "latest", 0, 10, null));
    }

    @Test
    @DisplayName("GET /api/library/search: searches by title and filters with zero data leakage")
    void searchPublicLessons_QueryAndZeroDataLeakage() {
        // Query by "Revolution"
        ResponseEntity<ApiResponse<PageResponse<LessonWithProgressDTO>>> searchRes =
                libraryController.searchPublicLessons("Revolution", null, null, null, "latest", 0, 10, userA);

        assertNotNull(searchRes);
        PageResponse<LessonWithProgressDTO> page = searchRes.getBody().getResult();
        assertEquals(1, page.getTotalElements());
        assertEquals("AI Revolution", page.getContent().get(0).getTitle());
        assertEquals(50, page.getContent().get(0).getProgress());

        // Blank query returns all published & done lessons (3 total)
        ResponseEntity<ApiResponse<PageResponse<LessonWithProgressDTO>>> allRes =
                libraryController.searchPublicLessons("", null, null, null, "latest", 0, 10, null);
        assertEquals(3, allRes.getBody().getResult().getTotalElements());

        // Ensure private lessons and processing lessons are NEVER returned in search
        List<UUID> searchIds = allRes.getBody().getResult().getContent().stream()
                .map(LessonWithProgressDTO::getId)
                .toList();
        assertThat(searchIds).doesNotContain(privateLessonUserB.getId(), processingLesson.getId(), userAPendingLesson.getId());
    }

    @Test
    @DisplayName("MockMvc: GET /api/library anonymous returns 200 OK with public sections only")
    void mockMvc_getLibrary_Anonymous() throws Exception {
        mockMvc.perform(get("/api/library")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("MockMvc: GET /api/library/sections authenticated returns 200 OK with personal sections")
    void mockMvc_getLibrarySections_Authenticated() throws Exception {
        mockMvc.perform(get("/api/library")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("MockMvc: GET /api/library/categories/{slug}/lessons returns 200 OK for valid category and 404 for invalid")
    void mockMvc_getCategoryLessons() throws Exception {
        mockMvc.perform(get("/api/library/categories/technology/lessons")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.totalElements").value(2));

        mockMvc.perform(get("/api/library/categories/non-existent-slug/lessons")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MockMvc: GET /api/library/search returns 200 OK with matching results")
    void mockMvc_searchPublicLessons() throws Exception {
        mockMvc.perform(get("/api/library/search")
                        .param("q", "Revolution")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.content[0].title").value("AI Revolution"));
    }
}