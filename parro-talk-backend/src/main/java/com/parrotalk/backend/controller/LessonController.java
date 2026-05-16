package com.parrotalk.backend.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.dto.LessonSearchRequest;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.PageResponse;

import com.parrotalk.backend.dto.SubmitLessonRequest;
import com.parrotalk.backend.dto.SubmitLessonResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.LearningService;
import com.parrotalk.backend.service.LessonService;
import com.parrotalk.backend.service.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lesson Controller.
 * 
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
@Slf4j
public class LessonController {

    /** Lesson Service */
    private final LessonService lessonService;

    /** SSE Service */
    private final SseService sseService;

    /** Learning Service */
    private final LearningService learningService;

    /**
     * Get list published lessons.
     * 
     * @param request Lesson search request
     * @param user    User
     * @return Page of lessons
     */
    @GetMapping
    public ResponseEntity<PageResponse<LessonWithProgressDTO>> listLessons(
            @ModelAttribute LessonSearchRequest request,
            @AuthenticationPrincipal User user) {
        long startTime = System.currentTimeMillis();
        PageResponse<LessonWithProgressDTO> response = lessonService.searchLessons(request, user);
        long endTime = System.currentTimeMillis();
        log.info("List lessons time: {} seconds", (endTime - startTime) / 1000.0);
        return ResponseEntity.ok(response);
    }

    /**
     * Get list own lessons.
     * 
     * @param request Lesson search request
     * @param user    User
     * @return Page of lessons
     */
    @GetMapping("/mine")
    public ResponseEntity<PageResponse<LessonWithProgressDTO>> listMyLessons(
            @ModelAttribute LessonSearchRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonService.searchMyLessons(request, user));
    }

    /**
     * Submit lesson.
     * 
     * @param lessonId Lesson ID
     * @param request  Submit lesson request
     * @param user     User
     * @return Submit lesson response
     */
    @PostMapping("/{lessonId}/submit")
    public ResponseEntity<SubmitLessonResponse> submitLesson(
            @PathVariable UUID lessonId,
            @RequestBody SubmitLessonRequest request,
            @AuthenticationPrincipal User user) {
        SubmitLessonResponse response = learningService.submitLessonProgress(user.getId(), lessonId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get lesson detail.
     * 
     * @param lessonId Lesson ID
     * @return Lesson
     */
    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> getLessonDetail(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonService.getAccessibleLessonDetail(lessonId, user));
    }

    /**
     * API to stream lesson status.
     * 
     * @param lessonId Lesson ID
     * @return SSE Emitter
     */
    @GetMapping(path = "/sse/{lessonId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLessonStatus(@PathVariable UUID lessonId) {
        SseEmitter emitter = sseService.connect(lessonId);

        // Send initial state immediately
        try {
            LessonResponse lesson = lessonService.getLessonStatus(lessonId);
            sseService.sendEvent(lessonId, Map.of(
                    "status", lesson.getStatus(),
                    "progress", lesson.getProgress(),
                    "step", "Connected"));

        } catch (Exception e) {
            log.error("Failed to send initial SSE event for lesson: {}", lessonId, e);
        }

        return emitter;
    }
}
