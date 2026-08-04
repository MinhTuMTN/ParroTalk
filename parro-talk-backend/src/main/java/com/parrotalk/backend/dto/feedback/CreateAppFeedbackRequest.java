package com.parrotalk.backend.dto.feedback;

import com.parrotalk.backend.constant.FeedbackCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload of {@code POST /api/feedback}.
 *
 * @param category    Feedback category
 * @param title       Short summary
 * @param description Detailed description
 */
public record CreateAppFeedbackRequest(
        @NotNull(message = "category is required") FeedbackCategory category,
        @NotBlank(message = "title is required") @Size(max = 150, message = "title must not exceed 150 characters") String title,
        @NotBlank(message = "description is required") @Size(max = 4000, message = "description must not exceed 4000 characters") String description) {
}
