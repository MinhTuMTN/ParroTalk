package com.parrotalk.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.dto.LessonCategoryAnalysisResponse;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.service.LessonCategoryClassificationService;
import com.parrotalk.backend.service.LessonService;

@ExtendWith(MockitoExtension.class)
class AdminLessonControllerTest {

    @Mock
    private LessonService lessonService;

    @Mock
    private LessonCategoryClassificationService lessonCategoryClassificationService;

    @InjectMocks
    private AdminLessonController adminLessonController;

    private UUID lessonId;

    @BeforeEach
    void setUp() {
        lessonId = UUID.randomUUID();
    }

    @Test
    void analyzeCategories_ReturnsOkWithResponse() {
        LessonCategoryAnalysisResponse mockResponse = LessonCategoryAnalysisResponse.builder()
                .lessonId(lessonId)
                .title("Test Lesson")
                .categories(List.of(
                        LessonCategoryAnalysisResponse.CategoryItem.builder()
                                .id(UUID.randomUUID())
                                .code("WORK_CAREER")
                                .nameEn("Work & Career")
                                .nameVi("Công việc & Nghề nghiệp")
                                .build()))
                .build();

        when(lessonCategoryClassificationService.classifyLessonCategories(lessonId)).thenReturn(mockResponse);

        ResponseEntity<LessonCategoryAnalysisResponse> response = adminLessonController.analyzeCategories(lessonId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(lessonCategoryClassificationService).classifyLessonCategories(lessonId);
    }

    @Test
    void getAdminLessons_DelegatesWithFiltersAndPagination() {
        UUID categoryId = UUID.randomUUID();
        PageResponse<LessonResponse> expectedPage = PageResponse.<LessonResponse>builder()
                .content(List.of(LessonResponse.builder().id(lessonId).title("English Pod").build()))
                .page(1)
                .size(20)
                .totalElements(21)
                .totalPages(2)
                .build();

        when(lessonService.searchAdminLessons("pod", LessonVisibilityStatus.PUBLISHED, categoryId, 1, 20))
                .thenReturn(expectedPage);

        ResponseEntity<PageResponse<LessonResponse>> response = adminLessonController.getAdminLessons(
                "pod", null, LessonVisibilityStatus.PUBLISHED, categoryId, 1, 20, null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedPage, response.getBody());
        verify(lessonService).searchAdminLessons("pod", LessonVisibilityStatus.PUBLISHED, categoryId, 1, 20);
    }

    @Test
    void getAdminLessons_FallsBackToQueryAndSizeAliases() {
        UUID categoryId = UUID.randomUUID();
        PageResponse<LessonResponse> expectedPage = PageResponse.<LessonResponse>builder()
                .content(List.of())
                .page(0)
                .size(15)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(lessonService.searchAdminLessons("news", LessonVisibilityStatus.HIDDEN, categoryId, 0, 15))
                .thenReturn(expectedPage);

        ResponseEntity<PageResponse<LessonResponse>> response = adminLessonController.getAdminLessons(
                null, "news", LessonVisibilityStatus.HIDDEN, categoryId, 0, 10, 15);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedPage, response.getBody());
        verify(lessonService).searchAdminLessons("news", LessonVisibilityStatus.HIDDEN, categoryId, 0, 15);
    }
}
