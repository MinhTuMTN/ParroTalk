package com.parrotalk.backend.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO returned after analyzing and classifying categories for a lesson.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonCategoryAnalysisResponse {

    /** Lesson ID */
    private UUID lessonId;

    /** Lesson title */
    private String title;

    /** List of categories */
    private List<CategoryItem> categories;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryItem {

        /** Category ID */
        private UUID id;

        /** Category code */
        private String code;

        /** Category name in English */
        private String nameEn;

        /** Category name in Vietnamese */
        private String nameVi;
    }
}
