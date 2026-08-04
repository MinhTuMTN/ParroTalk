package com.parrotalk.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.constant.FeedbackCategory;
import com.parrotalk.backend.constant.ModerationPriority;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.feedback.AppFeedbackResponse;
import com.parrotalk.backend.dto.moderation.ModerationEventResponse;
import com.parrotalk.backend.dto.moderation.ModerationUpdateRequest;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.AppFeedbackService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin App Feedback Controller.
 *
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppFeedbackController {

    /** App Feedback Service */
    private final AppFeedbackService appFeedbackService;

    /**
     * Search feedback.
     *
     * @param status     Triage status filter
     * @param category   Category filter
     * @param priority   Priority filter
     * @param assigneeId Assignee filter
     * @param page       Page index
     * @param size       Page size
     * @return Page of feedback
     */
    @GetMapping
    public ResponseEntity<PageResponse<AppFeedbackResponse>> getFeedback(
            @RequestParam(required = false) ModerationStatus status,
            @RequestParam(required = false) FeedbackCategory category,
            @RequestParam(required = false) ModerationPriority priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(appFeedbackService.search(status, category, priority, assigneeId, page, size));
    }

    /**
     * Get feedback detail.
     *
     * @param id Feedback ID
     * @return Feedback detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppFeedbackResponse> getFeedbackDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(appFeedbackService.getDetail(id));
    }

    /**
     * Update the triage state of a feedback.
     *
     * @param id      Feedback ID
     * @param request Requested changes
     * @param admin   Authenticated admin
     * @return Updated feedback
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AppFeedbackResponse> updateFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody ModerationUpdateRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(appFeedbackService.updateModeration(id, request, admin));
    }

    /**
     * Get the audit trail of a feedback.
     *
     * @param id Feedback ID
     * @return Ordered audit entries
     */
    @GetMapping("/{id}/events")
    public ResponseEntity<List<ModerationEventResponse>> getFeedbackEvents(@PathVariable UUID id) {
        return ResponseEntity.ok(appFeedbackService.getEvents(id));
    }
}
