package com.parrotalk.backend.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.SubmitLessonRequest;
import com.parrotalk.backend.dto.SubmitLessonResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.LearningService;
import com.parrotalk.backend.service.LessonService;
import com.parrotalk.backend.service.SseService;

import lombok.RequiredArgsConstructor;

/**
 * Lesson Controller.
 * 
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final SseService sseService;
    private final LearningService learningService;

    @GetMapping
    public ResponseEntity<PageResponse<LessonResponse>> listLessons(
            @AuthenticationPrincipal User user,
            @ModelAttribute LessonSearchRequest request) {
        return ResponseEntity.ok(lessonService.searchLessons(request));
    }

    @PostMapping("/{lessonId}/submit")
    public ResponseEntity<SubmitLessonResponse> submitLesson(
            @PathVariable UUID lessonId,
            @RequestBody SubmitLessonRequest request,
            @AuthenticationPrincipal User user) {
        SubmitLessonResponse response = learningService.submitLessonProgress(user.getId(), lessonId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> getLessonStatus(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(lessonService.getLessonResponse(lessonId));
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID lessonId) {
        lessonService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/{lessonId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLessonStatus(@PathVariable UUID lessonId) {
        return sseService.connect(lessonId);
    }
}
