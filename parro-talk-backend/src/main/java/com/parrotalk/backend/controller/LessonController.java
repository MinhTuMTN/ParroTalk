package com.parrotalk.backend.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.parrotalk.backend.dto.CategoryResponse;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.SubmitLessonRequest;
import com.parrotalk.backend.dto.SubmitLessonResponse;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.service.LearningService;
import com.parrotalk.backend.service.LessonService;
import com.parrotalk.backend.service.SseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final SseService sseService;
    private final LearningService learningService;
    private final LessonRepository lessonRepository;

    @GetMapping
    public ResponseEntity<PageResponse<LessonResponse>> listLessons(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Lesson> lessonPage = lessonRepository.searchLessons(q, categoryId, pageable);

        List<LessonResponse> content = lessonPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        PageResponse<LessonResponse> response = PageResponse.<LessonResponse>builder()
                .content(content)
                .page(lessonPage.getNumber())
                .size(lessonPage.getSize())
                .totalElements(lessonPage.getTotalElements())
                .totalPages(lessonPage.getTotalPages())
                .build();

        return ResponseEntity.ok(response);
    }

    private LessonResponse mapToResponse(Lesson lesson) {
        List<CategoryResponse> cats = null;
        if (lesson.getCategories() != null) {
            cats = lesson.getCategories().stream().map(c -> new CategoryResponse(c.getId(), c.getName()))
                    .collect(Collectors.toList());
        }
        return LessonResponse.builder()
                .id(lesson.getId())
                .status(lesson.getStatus())
                .progress(lesson.getProgress())
                .currentStep(lesson.getCurrentStep())
                .fileUrl(lesson.getFileUrl())
                .youtubeUrl(lesson.getYoutubeUrl())
                .mediaType(lesson.getMediaType().name())
                .sourceType(lesson.getSourceType().name())
                .createdAt(lesson.getCreatedAt())
                .title(lesson.getTitle())
                .content(lesson.getContent())
                .duration(lesson.getDuration())
                .categories(cats)
                .build();
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
