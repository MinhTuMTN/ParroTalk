package com.parrotalk.backend.controller;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.service.LessonService;
import com.parrotalk.backend.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final SseService sseService;

    @GetMapping
    public ResponseEntity<List<LessonResponse>> listLessons(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonService.getAllLessons(user));
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
