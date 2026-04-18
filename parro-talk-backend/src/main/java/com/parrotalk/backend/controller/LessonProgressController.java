package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.*;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Lesson Progress Controller.
 * Handle lesson progress operations.
 * 
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonProgressController {

    /** Lesson progress service */
    private final LessonProgressService lessonProgressService;

    /**
     * Get lesson progress.
     * 
     * @param lessonId Lesson ID.
     * @param user     User.
     * @return Lesson progress.
     */
    @GetMapping("/{lessonId}/progress")
    public ResponseEntity<LessonProgressResponse> getProgress(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonProgressService.getProgress(user, lessonId));
    }

    @GetMapping("/{lessonId}/progress/reset")
    public ResponseEntity<Void> resetProgress(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal User user) {
        lessonProgressService.resetProgress(user, lessonId);
        return ResponseEntity.ok().build();
    }

    /**
     * Submit answer.
     * 
     * @param lessonId Lesson ID.
     * @param request  Answer request.
     * @param user     User.
     * @return Draft segment response.
     */
    @PostMapping("/{lessonId}/answer")
    public ResponseEntity<DraftSegmentResponse> submitAnswer(
            @PathVariable UUID lessonId,
            @RequestBody AnswerRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonProgressService.submitAnswer(user, lessonId, request));
    }

    /**
     * Increment replay count.
     * 
     * @param lessonId  Lesson ID.
     * @param segmentId Segment ID.
     * @param user      User.
     * @return Void.
     */
    @PostMapping("/{lessonId}/segments/{segmentId}/replay")
    public ResponseEntity<Void> incrementReplay(
            @PathVariable UUID lessonId,
            @PathVariable UUID segmentId,
            @AuthenticationPrincipal User user) {
        lessonProgressService.incrementReplay(user.getId(), lessonId, segmentId);
        return ResponseEntity.ok().build();
    }

    /**
     * Increment hint count.
     * 
     * @param lessonId  Lesson ID.
     * @param segmentId Segment ID.
     * @param user      User.
     * @return Void.
     */
    @PostMapping("/{lessonId}/segments/{segmentId}/hint")
    public ResponseEntity<Void> incrementHint(
            @PathVariable UUID lessonId,
            @PathVariable UUID segmentId,
            @AuthenticationPrincipal User user) {
        lessonProgressService.incrementHint(user.getId(), lessonId, segmentId);
        return ResponseEntity.ok().build();
    }

    /**
     * Submit lesson.
     * 
     * @param lessonId Lesson ID.
     * @param user     User.
     * @return Submit lesson response.
     */
    @PostMapping("/{lessonId}/submit-lesson")
    public ResponseEntity<SubmitLessonResponse> submitLesson(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonProgressService.submitLesson(user, lessonId));
    }
}
