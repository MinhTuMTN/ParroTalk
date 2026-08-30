package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.dto.LessonCategoryAnalysisResponse;
import com.parrotalk.backend.dto.LlmCategoryClassificationResponse;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;

@ExtendWith(MockitoExtension.class)
class LessonCategoryClassificationServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private TranscriptionSegmentRepository transcriptionSegmentRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AiCoreClient aiCoreClient;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private TransactionTemplate transactionTemplate;

    private LessonCategoryClassificationService classificationService;

    private UUID lessonId;
    private Lesson lesson;
    private Category catWork;
    private Category catTech;
    private Category catHealth;
    private Category catEdu;

    @BeforeEach
    void setUp() {
        classificationService =
                new LessonCategoryClassificationService(lessonRepository, transcriptionSegmentRepository,
                        categoryRepository, aiCoreClient, cacheManager, transactionTemplate);

        lessonId = UUID.randomUUID();
        lesson = Lesson.builder().id(lessonId).title("The Future of Remote Work and AI").categories(new HashSet<>())
                .build();

        catWork = Category.builder().id(UUID.randomUUID()).name("Work & Career").slug("work-career")
                .description("Công việc & Nghề nghiệp").status(CmsItemStatus.ACTIVE).build();

        catTech = Category.builder().id(UUID.randomUUID()).name("Technology").slug("technology")
                .description("Công nghệ").status(CmsItemStatus.ACTIVE).build();

        catHealth = Category.builder().id(UUID.randomUUID()).name("Health").slug("health").description("Sức khỏe")
                .status(CmsItemStatus.ACTIVE).build();

        catEdu = Category.builder().id(UUID.randomUUID()).name("Education & Learning").slug("education-learning")
                .description("Giáo dục & Học tập").status(CmsItemStatus.ACTIVE).build();

        // Default mock for transactionTemplate to immediately run the action
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private void mockAiCoreClient(LlmCategoryClassificationResponse response) {
        when(aiCoreClient.callAndValidate(anyString(), anyString(), eq(LlmCategoryClassificationResponse.class), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Function<LlmCategoryClassificationResponse, List<Category>> validator = invocation.getArgument(3);
                    return validator.apply(response);
                });
    }

    @Test
    void classifyLessonCategories_Success_SingleCategory() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId)).thenReturn(
                List.of(
                        TranscriptionSegment.builder().text("Today we discuss modern careers.").displayOrder(1).build(),
                        TranscriptionSegment.builder().text("How to succeed at your job.").displayOrder(2).build()));
        when(categoryRepository.findByStatus(CmsItemStatus.ACTIVE)).thenReturn(List.of(catWork, catTech, catHealth));

        mockAiCoreClient(new LlmCategoryClassificationResponse(List.of("WORK_CAREER")));

        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        LessonCategoryAnalysisResponse response = classificationService.classifyLessonCategories(lessonId);

        assertNotNull(response);
        assertEquals(lessonId, response.getLessonId());
        assertEquals("The Future of Remote Work and AI", response.getTitle());
        assertEquals(1, response.getCategories().size());
        assertEquals("WORK_CAREER", response.getCategories().get(0).getCode());
        assertEquals("Work & Career", response.getCategories().get(0).getNameEn());
        assertEquals("Công việc & Nghề nghiệp", response.getCategories().get(0).getNameVi());

        // Verify lesson was saved with updated categories
        verify(lessonRepository).save(lesson);
        assertEquals(1, lesson.getCategories().size());
        assertTrue(lesson.getCategories().contains(catWork));
        verify(mockCache).evict(lessonId);
    }

    @Test
    void classifyLessonCategories_Success_MultipleCategories() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId)).thenReturn(
                List.of(
                        TranscriptionSegment.builder().text("Artificial intelligence is changing software engineering.")
                                .displayOrder(1).build()));
        when(categoryRepository.findByStatus(CmsItemStatus.ACTIVE)).thenReturn(List.of(catWork, catTech, catHealth));

        mockAiCoreClient(new LlmCategoryClassificationResponse(List.of("WORK_CAREER", "TECHNOLOGY")));

        LessonCategoryAnalysisResponse response = classificationService.classifyLessonCategories(lessonId);

        assertNotNull(response);
        assertEquals(2, response.getCategories().size());
        assertEquals(2, lesson.getCategories().size());
        assertTrue(lesson.getCategories().contains(catWork));
        assertTrue(lesson.getCategories().contains(catTech));
    }

    @Test
    void classifyLessonCategories_ReplacesExistingCategories_PreservesMasterData() {
        // Lesson initially has catHealth
        lesson.getCategories().add(catHealth);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId)).thenReturn(
                List.of(TranscriptionSegment.builder().text("Learning computer programming.").displayOrder(1).build()));
        when(categoryRepository.findByStatus(CmsItemStatus.ACTIVE))
                .thenReturn(List.of(catWork, catTech, catHealth, catEdu));

        mockAiCoreClient(new LlmCategoryClassificationResponse(List.of("TECHNOLOGY", "EDUCATION_LEARNING")));

        classificationService.classifyLessonCategories(lessonId);

        // Lesson should now have catTech and catEdu, catHealth is removed from lesson
        assertEquals(2, lesson.getCategories().size());
        assertTrue(lesson.getCategories().contains(catTech));
        assertTrue(lesson.getCategories().contains(catEdu));
        // Ensure category master entities are not deleted
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void classifyLessonCategories_LessonNotFound_Throws404() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        ParroTalkException ex =
                assertThrows(ParroTalkException.class, () -> classificationService.classifyLessonCategories(lessonId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatusCode());
        assertEquals("Lesson not found.", ex.getMessage());
        verify(aiCoreClient, never()).callAndValidate(anyString(), anyString(), any(Class.class), any());
    }

    @Test
    void classifyLessonCategories_LessonTitleBlank_Throws400() {
        lesson.setTitle("   ");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        ParroTalkException ex =
                assertThrows(ParroTalkException.class, () -> classificationService.classifyLessonCategories(lessonId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
        assertEquals("Lesson title is missing or blank.", ex.getMessage());
        verify(aiCoreClient, never()).callAndValidate(anyString(), anyString(), any(Class.class), any());
    }

    @Test
    void classifyLessonCategories_LessonTranscriptEmpty_Throws400() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId)).thenReturn(List.of());

        ParroTalkException ex =
                assertThrows(ParroTalkException.class, () -> classificationService.classifyLessonCategories(lessonId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
        assertEquals("Lesson transcript is empty or not available.", ex.getMessage());
        verify(aiCoreClient, never()).callAndValidate(anyString(), anyString(), any(Class.class), any());
    }

    @Test
    void classifyLessonCategories_NoAllowedCategoriesInDb_Throws400() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId)).thenReturn(
                List.of(TranscriptionSegment.builder().text("Some transcript text").displayOrder(1).build()));
        when(categoryRepository.findByStatus(CmsItemStatus.ACTIVE)).thenReturn(List.of());

        ParroTalkException ex =
                assertThrows(ParroTalkException.class, () -> classificationService.classifyLessonCategories(lessonId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
        assertEquals("No active allowed categories available for classification.", ex.getMessage());
        verify(aiCoreClient, never()).callAndValidate(anyString(), anyString(), any(Class.class), any());
    }

    @Test
    void classifyLessonCategories_AiReturnsMoreThan3Categories_ThrowsUnprocessableEntity() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId))
                .thenReturn(List.of(TranscriptionSegment.builder().text("Full transcript.").displayOrder(1).build()));
        when(categoryRepository.findByStatus(CmsItemStatus.ACTIVE))
                .thenReturn(List.of(catWork, catTech, catHealth, catEdu));

        mockAiCoreClient(
                new LlmCategoryClassificationResponse(
                        List.of("WORK_CAREER", "TECHNOLOGY", "HEALTH", "EDUCATION_LEARNING")));

        ParroTalkException ex =
                assertThrows(ParroTalkException.class, () -> classificationService.classifyLessonCategories(lessonId));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getHttpStatusCode());
        assertTrue(ex.getMessage().contains("Maximum allowed is 3"));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void classifyLessonCategories_AiReturns0Categories_ThrowsUnprocessableEntity() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId))
                .thenReturn(List.of(TranscriptionSegment.builder().text("Full transcript.").displayOrder(1).build()));
        when(categoryRepository.findByStatus(CmsItemStatus.ACTIVE)).thenReturn(List.of(catWork, catTech));

        mockAiCoreClient(new LlmCategoryClassificationResponse(List.of()));

        ParroTalkException ex =
                assertThrows(ParroTalkException.class, () -> classificationService.classifyLessonCategories(lessonId));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getHttpStatusCode());
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void classifyLessonCategories_AiReturnsUnknownCategoryCode_ThrowsUnprocessableEntity() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId))
                .thenReturn(List.of(TranscriptionSegment.builder().text("Full transcript.").displayOrder(1).build()));
        when(categoryRepository.findByStatus(CmsItemStatus.ACTIVE)).thenReturn(List.of(catWork, catTech));

        mockAiCoreClient(new LlmCategoryClassificationResponse(List.of("SPACE_ASTRONOMY")));

        ParroTalkException ex =
                assertThrows(ParroTalkException.class, () -> classificationService.classifyLessonCategories(lessonId));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getHttpStatusCode());
        assertTrue(ex.getMessage().contains("unauthorized or unknown category code"));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void classifyLessonCategories_AiFailure_Throws500() {
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(transcriptionSegmentRepository.findByLessonIdOrderByDisplayOrderAsc(lessonId))
                .thenReturn(List.of(TranscriptionSegment.builder().text("Full transcript.").displayOrder(1).build()));
        when(categoryRepository.findByStatus(CmsItemStatus.ACTIVE)).thenReturn(List.of(catWork, catTech));

        when(aiCoreClient.callAndValidate(anyString(), anyString(), eq(LlmCategoryClassificationResponse.class), any()))
                .thenThrow(new RuntimeException("Connection timed out to LLM provider"));

        ParroTalkException ex =
                assertThrows(ParroTalkException.class, () -> classificationService.classifyLessonCategories(lessonId));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatusCode());
        assertTrue(ex.getMessage().contains("Failed to analyze lesson categories with AI"));
        verify(lessonRepository, never()).save(any());
    }
}
