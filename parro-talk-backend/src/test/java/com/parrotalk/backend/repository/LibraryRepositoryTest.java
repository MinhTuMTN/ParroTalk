package com.parrotalk.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserLessonProgress;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class LibraryRepositoryTest {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LibrarySectionRepository librarySectionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLessonProgressRepository progressRepository;

    private User userA;
    private User userB;
    private Category techCategory;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .email("usera@example.com")
                .fullName("User A")
                .password("hashed_pwd")
                .role(Role.USER)
                .build());

        userB = userRepository.save(User.builder()
                .email("userb@example.com")
                .fullName("User B")
                .password("hashed_pwd")
                .role(Role.USER)
                .build());

        techCategory = categoryRepository.save(Category.builder()
                .name("Technology")
                .slug("technology")
                .sortOrder(1)
                .status(CmsItemStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("LibrarySectionRepository: ordering and active filter")
    void testLibrarySectionRepository() {
        LibrarySection s1 = librarySectionRepository.save(LibrarySection.builder()
                .title("Sec 1")
                .type(SectionType.IN_PROGRESS)
                .orderIndex(2)
                .isActive(true)
                .build());

        LibrarySection s2 = librarySectionRepository.save(LibrarySection.builder()
                .title("Sec 2")
                .type(SectionType.PROGRAM)
                .program("6 Minute English")
                .orderIndex(1)
                .isActive(true)
                .build());

        LibrarySection s3 = librarySectionRepository.save(LibrarySection.builder()
                .title("Sec 3")
                .type(SectionType.CATEGORY)
                .category(techCategory)
                .orderIndex(3)
                .isActive(false)
                .build());

        List<LibrarySection> allOrdered = librarySectionRepository.findAllByOrderByOrderIndexAsc();
        assertThat(allOrdered).hasSize(3);
        assertThat(allOrdered.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(allOrdered.get(1).getOrderIndex()).isEqualTo(2);
        assertThat(allOrdered.get(2).getOrderIndex()).isEqualTo(3);

        List<LibrarySection> activeOnly = librarySectionRepository.findByIsActiveTrueOrderByOrderIndexAsc();
        assertThat(activeOnly).hasSize(2);
        assertThat(activeOnly.get(0).getTitle()).isEqualTo("Sec 2");
        assertThat(activeOnly.get(1).getTitle()).isEqualTo("Sec 1");
    }

    @Test
    @DisplayName("LessonRepository: findContinueLearning only returns in-progress lessons (0 < lastProgress < 1.0) for user")
    void testFindContinueLearning() {
        Lesson l1 = lessonRepository.save(Lesson.builder()
                .title("In Progress Lesson")
                .fileUrl("https://example.com/audio1.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .build());

        Lesson l2 = lessonRepository.save(Lesson.builder()
                .title("Completed Lesson")
                .fileUrl("https://example.com/audio2.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .build());

        progressRepository.save(UserLessonProgress.builder()
                .id(new UserLessonProgress.UserLessonProgressId(userA.getId(), l1.getId()))
                .user(userA)
                .lesson(l1)
                .lastProgress(0.45)
                .build());

        progressRepository.save(UserLessonProgress.builder()
                .id(new UserLessonProgress.UserLessonProgressId(userA.getId(), l2.getId()))
                .user(userA)
                .lesson(l2)
                .lastProgress(1.0)
                .build());

        List<LessonWithProgressDTO> progressList = lessonRepository.findContinueLearning(userA.getId(), PageRequest.of(0, 10));
        assertThat(progressList).hasSize(1);
        assertThat(progressList.get(0).getTitle()).isEqualTo("In Progress Lesson");
        assertThat(progressList.get(0).getProgress()).isEqualTo(45);

        // User B has no progress
        List<LessonWithProgressDTO> userBList = lessonRepository.findContinueLearning(userB.getId(), PageRequest.of(0, 10));
        assertThat(userBList).isEmpty();
    }

    @Test
    @DisplayName("LessonRepository: findUserProcessing and findUserUploaded filter by owner and status")
    void testFindUserProcessingAndUploaded() {
        lessonRepository.save(Lesson.builder()
                .title("Processing Lesson")
                .fileUrl("https://example.com/proc.mp3")
                .ownerId(userA.getId())
                .status(LessonStatus.PROCESSING)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .build());

        lessonRepository.save(Lesson.builder()
                .title("Uploaded Done Lesson")
                .fileUrl("https://example.com/done.mp3")
                .ownerId(userA.getId())
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .build());

        List<LessonWithProgressDTO> processing = lessonRepository.findUserProcessing(userA.getId(), PageRequest.of(0, 10));
        assertThat(processing).hasSize(1);
        assertThat(processing.get(0).getTitle()).isEqualTo("Processing Lesson");

        List<LessonWithProgressDTO> uploaded = lessonRepository.findUserUploaded(userA.getId(), PageRequest.of(0, 10));
        assertThat(uploaded).hasSize(1);
        assertThat(uploaded.get(0).getTitle()).isEqualTo("Uploaded Done Lesson");

        // User B has none
        assertThat(lessonRepository.findUserProcessing(userB.getId(), PageRequest.of(0, 10))).isEmpty();
        assertThat(lessonRepository.findUserUploaded(userB.getId(), PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    @DisplayName("LessonRepository: findByCategory, findByProgram, findCategoryLessonsBySlug, and searchPublicLessons")
    void testLibraryFilteringAndSearch() {
        Lesson l1 = lessonRepository.save(Lesson.builder()
                .title("AI Revolution in Tech")
                .fileUrl("https://example.com/ai.mp3")
                .categories(Set.of(techCategory))
                .program("TED Talks")
                .source("Youtube")
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .build());

        Lesson l2 = lessonRepository.save(Lesson.builder()
                .title("English Vocabulary for Travel")
                .fileUrl("https://example.com/travel.mp3")
                .program("6 Minute English")
                .source("BBC")
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .build());

        // Private / Processing lesson should NOT appear in public queries
        lessonRepository.save(Lesson.builder()
                .title("Private AI draft")
                .fileUrl("https://example.com/private.mp3")
                .categories(Set.of(techCategory))
                .program("TED Talks")
                .source("Youtube")
                .status(LessonStatus.PROCESSING)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .build());

        // Category filter
        List<LessonWithProgressDTO> byCat = lessonRepository.findByCategory(techCategory.getId(), userA.getId(), PageRequest.of(0, 10));
        assertThat(byCat).hasSize(1);
        assertThat(byCat.get(0).getTitle()).isEqualTo("AI Revolution in Tech");

        // Program filter
        List<LessonWithProgressDTO> byProg = lessonRepository.findByProgram("6 Minute English", userA.getId(), PageRequest.of(0, 10));
        assertThat(byProg).hasSize(1);
        assertThat(byProg.get(0).getTitle()).isEqualTo("English Vocabulary for Travel");

        // Category slug query with program/source filters
        Page<LessonWithProgressDTO> slugPage = lessonRepository.findCategoryLessonsBySlug("technology", "TED Talks", "Youtube", userA.getId(), PageRequest.of(0, 10));
        assertThat(slugPage.getContent()).hasSize(1);
        assertThat(slugPage.getContent().get(0).getTitle()).isEqualTo("AI Revolution in Tech");

        // Search public lessons
        Page<LessonWithProgressDTO> searchAI = lessonRepository.searchPublicLessons("revolution", null, null, null, userA.getId(), PageRequest.of(0, 10));
        assertThat(searchAI.getContent()).hasSize(1);
        assertThat(searchAI.getContent().get(0).getTitle()).isEqualTo("AI Revolution in Tech");

        Page<LessonWithProgressDTO> searchEmpty = lessonRepository.searchPublicLessons("NonExistent", null, null, null, userA.getId(), PageRequest.of(0, 10));
        assertThat(searchEmpty.getContent()).isEmpty();
    }

    @Test
    @DisplayName("LessonRepository: batch queries (findBatchCategoryLessons, findBatchProgramLessons, findBatchUserLessons)")
    void testBatchQueries() {
        Lesson l1 = lessonRepository.save(Lesson.builder()
                .title("Batch Category Lesson")
                .fileUrl("https://example.com/cat.mp3")
                .categories(Set.of(techCategory))
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .build());

        Lesson l2 = lessonRepository.save(Lesson.builder()
                .title("Batch Program Lesson")
                .fileUrl("https://example.com/prog.mp3")
                .program("TED Talks")
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .build());

        // In progress lesson for User A
        Lesson l3 = lessonRepository.save(Lesson.builder()
                .title("Batch In-Progress Lesson")
                .fileUrl("https://example.com/prog-user.mp3")
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .build());

        progressRepository.save(UserLessonProgress.builder()
                .id(new UserLessonProgress.UserLessonProgressId(userA.getId(), l3.getId()))
                .user(userA)
                .lesson(l3)
                .lastProgress(0.7)
                .build());

        // User A uploaded lesson
        lessonRepository.save(Lesson.builder()
                .title("Batch Uploaded Lesson")
                .fileUrl("https://example.com/up.mp3")
                .ownerId(userA.getId())
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .build());

        // Test Category Batch
        var catProjections = lessonRepository.findBatchCategoryLessons(List.of(techCategory.getId()), 10);
        assertThat(catProjections).isNotEmpty();
        assertThat(catProjections.get(0).getTitle()).isEqualTo("Batch Category Lesson");
        assertThat(catProjections.get(0).getSectionKey()).isEqualTo(techCategory.getId().toString());

        // Test Program Batch
        var progProjections = lessonRepository.findBatchProgramLessons(List.of("TED Talks"), 10);
        assertThat(progProjections).isNotEmpty();
        assertThat(progProjections.get(0).getTitle()).isEqualTo("Batch Program Lesson");
        assertThat(progProjections.get(0).getSectionKey()).isEqualTo("TED Talks");

        // Test User Batch
        var userProjections = lessonRepository.findBatchUserLessons(userA.getId(), 10);
        assertThat(userProjections).hasSize(2);
        var inProg = userProjections.stream().filter(p -> "IN_PROGRESS".equals(p.getSectionKey())).findFirst();
        assertThat(inProg).isPresent();
        assertThat(inProg.get().getTitle()).isEqualTo("Batch In-Progress Lesson");
        assertThat(inProg.get().getProgress()).isEqualTo(70.0);

        var uploaded = userProjections.stream().filter(p -> "USER_UPLOADED".equals(p.getSectionKey())).findFirst();
        assertThat(uploaded).isPresent();
        assertThat(uploaded.get().getTitle()).isEqualTo("Batch Uploaded Lesson");
    }
}

