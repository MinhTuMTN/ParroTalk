package com.parrotalk.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.dto.AdminCreateLessonRequest;
import com.parrotalk.backend.dto.AdminUpdateLessonRequest;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.UpdateLessonSegmentsRequest;
import com.parrotalk.backend.dto.UpdateLessonStatusRequest;
import com.parrotalk.backend.service.LessonService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Lesson Controller (for Admin).
 * 
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/admin/lessons")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminLessonController {

    /** Lesson Service */
    private final LessonService lessonService;

    /**
     * Get list lessons.
     * 
     * @param search Search query
     * @param status Lesson status
     * @param page   Page number
     * @param limit  Page limit
     * @return Page of Lessons
     */
    @GetMapping
    public ResponseEntity<PageResponse<LessonResponse>> getAdminLessons(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LessonVisibilityStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(lessonService.searchAdminLessons(search, status, page, limit));
    }

    /**
     * Create lesson.
     * 
     * @param request Create lesson request
     * @return Lesson
     */
    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(
            @Valid @RequestBody AdminCreateLessonRequest request) {
        return ResponseEntity.ok(lessonService.createAdminLesson(request));
    }

    /**
     * Update lesson.
     * 
     * @param id      Lesson ID
     * @param request Update lesson request
     * @return Lesson
     */
    @PutMapping("/{id}")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateLessonRequest request) {
        return ResponseEntity.ok(lessonService.updateAdminLesson(id, request));
    }

    /**
     * Delete lesson.
     * 
     * @param id Lesson ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get lesson detail.
     * 
     * @param id Lesson ID
     * @return Lesson
     */
    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getLessonDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getAdminLessonDetail(id));
    }

    /**
     * Update lesson segments.
     * 
     * @param id      Lesson ID
     * @param request Update lesson segments request
     * @return Lesson
     */
    @PutMapping("/{id}/segments")
    public ResponseEntity<LessonResponse> updateLessonSegments(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLessonSegmentsRequest request) {
        return ResponseEntity.ok(lessonService.updateLessonSegments(id, request.getSegments()));
    }

    /**
     * Update lesson status.
     * 
     * @param id      Lesson ID
     * @param request Update lesson status request
     * @return Lesson
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<LessonResponse> updateLessonStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLessonStatusRequest request) {
        return ResponseEntity.ok(lessonService.updateLessonVisibility(id, request.getStatus()));
    }
}
