package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.practice.AnswerResultDto;
import com.parrotalk.backend.dto.practice.AnswerSubmissionDto;
import com.parrotalk.backend.dto.practice.PracticeResultDto;
import com.parrotalk.backend.dto.practice.PracticeSessionDto;
import com.parrotalk.backend.dto.practice.PracticeStatisticsDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.PracticeService;
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
@RequestMapping("/api/v1/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/session")
    public ResponseEntity<PracticeSessionDto> startSession(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(practiceService.generateSession(user.getId()));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<PracticeSessionDto> getSession(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(practiceService.getSession(user.getId(), sessionId));
    }

    @PostMapping("/answer")
    public ResponseEntity<AnswerResultDto> submitAnswer(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AnswerSubmissionDto request) {
        return ResponseEntity.ok(practiceService.submitAnswer(user.getId(), request));
    }

    @GetMapping("/result/{sessionId}")
    public ResponseEntity<PracticeResultDto> getResult(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(practiceService.getSessionResult(user.getId(), sessionId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<PracticeStatisticsDto> getStatistics(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(practiceService.getStatistics(user.getId()));
    }
}
