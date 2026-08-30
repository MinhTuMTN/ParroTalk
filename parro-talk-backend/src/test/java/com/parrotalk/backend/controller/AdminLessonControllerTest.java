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

import com.parrotalk.backend.dto.LessonCategoryAnalysisResponse;
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
}
