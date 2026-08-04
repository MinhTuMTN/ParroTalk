package com.parrotalk.backend.dto.feedback;

import java.time.LocalDateTime;
import java.util.UUID;

import com.parrotalk.backend.constant.FeedbackCategory;
import com.parrotalk.backend.dto.moderation.ModerationStateResponse;
import com.parrotalk.backend.dto.moderation.UserSummaryResponse;
import com.parrotalk.backend.entity.AppFeedback;

/**
 * App feedback as returned to its author and to admins.
 *
 * @param id          Feedback id
 * @param category    Feedback category
 * @param title       Short summary
 * @param description Detailed description
 * @param submitter   Who submitted the feedback
 * @param moderation  Triage state
 * @param createdAt   Submission time
 */
public record AppFeedbackResponse(
        UUID id,
        FeedbackCategory category,
        String title,
        String description,
        UserSummaryResponse submitter,
        ModerationStateResponse moderation,
        LocalDateTime createdAt) {

    /**
     * Map an app feedback entity.
     *
     * @param feedback App feedback
     * @return Response projection
     */
    public static AppFeedbackResponse from(AppFeedback feedback) {
        return new AppFeedbackResponse(
                feedback.getId(),
                feedback.getCategory(),
                feedback.getTitle(),
                feedback.getDescription(),
                UserSummaryResponse.from(feedback.getUser()),
                ModerationStateResponse.from(feedback.getModerationState()),
                feedback.getCreatedAt());
    }
}
