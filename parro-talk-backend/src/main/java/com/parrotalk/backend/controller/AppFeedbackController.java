package com.parrotalk.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.feedback.AppFeedbackResponse;
import com.parrotalk.backend.dto.feedback.CreateAppFeedbackRequest;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.AppFeedbackService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * App Feedback Controller.
 *
 * <p>
 * Standalone resource: not nested under any lesson route.
 * </p>
 *
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Validated
public class AppFeedbackController {

    /** App Feedback Service */
    private final AppFeedbackService appFeedbackService;

    /**
     * Submit feedback about the application.
     *
     * @param request Feedback payload
     * @param user    Authenticated user
     * @return Created feedback
     */
    @PostMapping
    public ResponseEntity<AppFeedbackResponse> createFeedback(
            @Valid @RequestBody CreateAppFeedbackRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(appFeedbackService.create(user, request));
    }

    /**
     * List the feedback the caller submitted.
     *
     * @param page Page index
     * @param size Page size
     * @param user Authenticated user
     * @return Page of own feedback
     */
    @GetMapping
    public ResponseEntity<PageResponse<AppFeedbackResponse>> listMyFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(appFeedbackService.listOwnFeedback(user, page, size));
    }
}
