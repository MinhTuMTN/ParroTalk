package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.writing.WritingAnalyzeRequestDto;
import com.parrotalk.backend.dto.writing.WritingDraftUpdateRequestDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto;
import com.parrotalk.backend.dto.writing.WritingSessionSummaryDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.WritingAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller for the AI Writing Assistant feature.
 */
@RestController
@RequestMapping("/api/writing")
@RequiredArgsConstructor
public class WritingAssistantController {

    private final WritingAssistantService writingService;

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u && u.getId() != null) {
            return u;
        }
        return null;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<WritingSessionResponseDto>> analyze(
            @Valid @RequestBody WritingAnalyzeRequestDto request) {
        User user = getCurrentUser();
        WritingSessionResponseDto response = writingService.analyzeAndSave(request, user);
        return ResponseEntity.ok(ApiResponse.<WritingSessionResponseDto>builder()
                .code(1000)
                .result(response)
                .build());
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<PageResponse<WritingSessionSummaryDto>>> getSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = getCurrentUser();
        PageResponse<WritingSessionSummaryDto> response = writingService.getUserSessions(user, page, size);
        return ResponseEntity.ok(ApiResponse.<PageResponse<WritingSessionSummaryDto>>builder()
                .code(1000)
                .result(response)
                .build());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<WritingSessionResponseDto>> getSessionDetail(
            @PathVariable UUID sessionId) {
        User user = getCurrentUser();
        WritingSessionResponseDto response = writingService.getSessionDetail(sessionId, user);
        return ResponseEntity.ok(ApiResponse.<WritingSessionResponseDto>builder()
                .code(1000)
                .result(response)
                .build());
    }

    @PatchMapping("/sessions/{sessionId}/draft")
    public ResponseEntity<ApiResponse<WritingSessionResponseDto>> updateDraft(
            @PathVariable UUID sessionId,
            @Valid @RequestBody WritingDraftUpdateRequestDto request) {
        User user = getCurrentUser();
        WritingSessionResponseDto response = writingService.updateUserDraft(sessionId, request.getUserDraft(), user);
        return ResponseEntity.ok(ApiResponse.<WritingSessionResponseDto>builder()
                .code(1000)
                .result(response)
                .build());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable UUID sessionId) {
        User user = getCurrentUser();
        writingService.deleteSession(sessionId, user);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(1000)
                .message("Deleted writing session successfully.")
                .build());
    }
}

