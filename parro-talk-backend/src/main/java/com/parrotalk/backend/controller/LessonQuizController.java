package com.parrotalk.backend.controller;

import com.parrotalk.backend.constant.QuizMode;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.quiz.CreateQuizAttemptRequest;
import com.parrotalk.backend.dto.quiz.LessonQuizAttemptResponse;
import com.parrotalk.backend.dto.quiz.LessonQuizAttemptSummaryDto;
import com.parrotalk.backend.dto.quiz.LessonQuizOverviewResponse;
import com.parrotalk.backend.dto.quiz.QuizAnswerResultDto;
import com.parrotalk.backend.dto.quiz.RegenerateQuizRequest;
import com.parrotalk.backend.dto.quiz.SubmitQuizAnswerRequest;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.LessonQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/lessons/{lessonId}/quiz")
@RequiredArgsConstructor
public class LessonQuizController {

    private final LessonQuizService lessonQuizService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<LessonQuizOverviewResponse>> getOverview(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal User user) {
        LessonQuizOverviewResponse result = lessonQuizService.getOverview(lessonId, user);
        return ResponseEntity.ok(ApiResponse.<LessonQuizOverviewResponse>builder()
                .code(1000)
                .result(result)
                .build());
    }

    @PostMapping("/attempts")
    public ResponseEntity<ApiResponse<LessonQuizAttemptResponse>> createOrResumeAttempt(
            @PathVariable UUID lessonId,
            @RequestBody(required = false) CreateQuizAttemptRequest request,
            @AuthenticationPrincipal User user) {
        LessonQuizAttemptResponse result = lessonQuizService.createOrResumeAttempt(lessonId, request, user);
        return ResponseEntity.ok(ApiResponse.<LessonQuizAttemptResponse>builder()
                .code(1000)
                .result(result)
                .build());
    }

    @PostMapping("/regenerate")
    public ResponseEntity<ApiResponse<LessonQuizAttemptResponse>> regenerateQuiz(
            @PathVariable UUID lessonId,
            @Valid @RequestBody(required = false) RegenerateQuizRequest request,
            @AuthenticationPrincipal User user) {
        QuizMode mode = request != null ? request.getMode() : QuizMode.MIXED;
        LessonQuizAttemptResponse result = lessonQuizService.regenerateQuiz(lessonId, mode, user);
        return ResponseEntity.ok(ApiResponse.<LessonQuizAttemptResponse>builder()
                .code(1000)
                .result(result)
                .build());
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<ApiResponse<LessonQuizAttemptResponse>> getAttempt(
            @PathVariable UUID lessonId,
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal User user) {
        LessonQuizAttemptResponse result = lessonQuizService.getAttempt(lessonId, attemptId, user);
        return ResponseEntity.ok(ApiResponse.<LessonQuizAttemptResponse>builder()
                .code(1000)
                .result(result)
                .build());
    }

    @PostMapping("/attempts/{attemptId}/answer")
    public ResponseEntity<ApiResponse<QuizAnswerResultDto>> submitAnswer(
            @PathVariable UUID lessonId,
            @PathVariable UUID attemptId,
            @Valid @RequestBody SubmitQuizAnswerRequest request,
            @AuthenticationPrincipal User user) {
        QuizAnswerResultDto result = lessonQuizService.submitAnswer(lessonId, attemptId, request, user);
        return ResponseEntity.ok(ApiResponse.<QuizAnswerResultDto>builder()
                .code(1000)
                .result(result)
                .build());
    }

    @PostMapping("/attempts/{attemptId}/complete")
    public ResponseEntity<ApiResponse<LessonQuizAttemptSummaryDto>> completeAttempt(
            @PathVariable UUID lessonId,
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal User user) {
        LessonQuizAttemptSummaryDto result = lessonQuizService.completeAttempt(lessonId, attemptId, user);
        return ResponseEntity.ok(ApiResponse.<LessonQuizAttemptSummaryDto>builder()
                .code(1000)
                .result(result)
                .build());
    }
}
