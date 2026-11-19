package com.parrotalk.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.AnswerRequest;
import com.parrotalk.backend.dto.DraftSegmentResponse;
import com.parrotalk.backend.dto.LessonProgressResponse;
import com.parrotalk.backend.dto.SubmitLessonResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.LessonProgressService;

import lombok.RequiredArgsConstructor;

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
     * @param mode     Optional dictation mode (defaults to FULL).
     * @param user     User.
     * @return Lesson progress.
     */
    @GetMapping("/{lessonId}/progress")
    public ResponseEntity<LessonProgressResponse> getProgress(
            @PathVariable UUID lessonId,
            @RequestParam(required = false) DictationMode mode,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonProgressService.getProgress(user, lessonId, mode));
    }

    @GetMapping("/{lessonId}/progress/reset")
    public ResponseEntity<Void> resetProgress(
            @PathVariable UUID lessonId,
            @RequestParam(required = false) DictationMode mode,
            @AuthenticationPrincipal User user) {
        lessonProgressService.resetProgress(user, lessonId, mode);
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
     * @param mode      Optional dictation mode (defaults to FULL).
     * @param user      User.
     * @return Void.
     */
    @PostMapping("/{lessonId}/segments/{segmentId}/replay")
    public ResponseEntity<Void> incrementReplay(
            @PathVariable UUID lessonId,
            @PathVariable UUID segmentId,
            @RequestParam(required = false) DictationMode mode,
            @AuthenticationPrincipal User user) {
        lessonProgressService.incrementReplay(user.getId(), lessonId, segmentId, mode);
        return ResponseEntity.ok().build();
    }

    /**
     * Increment hint count.
     * 
     * @param lessonId  Lesson ID.
     * @param segmentId Segment ID.
     * @param mode      Optional dictation mode (defaults to FULL).
     * @param user      User.
     * @return Void.
     */
    @PostMapping("/{lessonId}/segments/{segmentId}/hint")
    public ResponseEntity<Void> incrementHint(
            @PathVariable UUID lessonId,
            @PathVariable UUID segmentId,
            @RequestParam(required = false) DictationMode mode,
            @AuthenticationPrincipal User user) {
        lessonProgressService.incrementHint(user.getId(), lessonId, segmentId, mode);
        return ResponseEntity.ok().build();
    }

    /**
     * Submit lesson.
     * 
     * @param lessonId Lesson ID.
     * @param mode     Optional dictation mode (defaults to FULL).
     * @param user     User.
     * @return Submit lesson response.
     */
    @PostMapping("/{lessonId}/submit-lesson")
    public ResponseEntity<SubmitLessonResponse> submitLesson(
            @PathVariable UUID lessonId,
            @RequestParam(required = false) DictationMode mode,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonProgressService.submitLesson(user, lessonId, mode));
    }
}
