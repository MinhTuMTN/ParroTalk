package com.parrotalk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for AI category description suggestion.
 */
@Getter
@Setter
public class CategoryAiSuggestDescriptionRequest {

    /** Category name to generate description for. */
    @NotBlank(message = "Category name is required")
    private String categoryName;

    /** Target learner profile, for example: beginner, intermediate, IELTS learner. */
    private String targetLearner;

    /** Output language, for example: en or vi. */
    private String language;
}
