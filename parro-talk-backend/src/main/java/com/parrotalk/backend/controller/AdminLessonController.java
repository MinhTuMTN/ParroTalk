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
import com.parrotalk.backend.dto.LessonCategoryAnalysisResponse;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.SegmentTranslationResponse;
import com.parrotalk.backend.dto.TranslationSummaryResponse;
import com.parrotalk.backend.dto.UpdateLessonInfoRequest;
import com.parrotalk.backend.dto.UpdateLessonSegmentsRequest;
import com.parrotalk.backend.dto.UpdateLessonStatusRequest;
import com.parrotalk.backend.service.LessonCategoryClassificationService;
import com.parrotalk.backend.service.LessonService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    /** Lesson Category Classification Service */
    private final LessonCategoryClassificationService lessonCategoryClassificationService;

    /**
     * Get list lessons.
     * 
     * @param search     Search query
     * @param query      Alternative search query alias
     * @param status     Lesson visibility status
     * @param categoryId Optional category ID filter
     * @param page       Page number
     * @param limit      Page limit
     * @param size       Optional alternative page size alias
     * @return Page of Lessons
     */
    @GetMapping
    public ResponseEntity<PageResponse<LessonResponse>> getAdminLessons(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) LessonVisibilityStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Integer size) {
        String effectiveQuery = (search != null && !search.isBlank()) ? search : query;
        int effectiveLimit = (size != null && size > 0) ? size : limit;
        return ResponseEntity
                .ok(lessonService.searchAdminLessons(effectiveQuery, status, categoryId, page, effectiveLimit));
    }

    /**
     * Create lesson. (TODO: Create lesson, segment and user lesson progress together)
     * 
     * @param request Create lesson request
     * @return Lesson
     */
    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(@Valid @RequestBody AdminCreateLessonRequest request) {
        return ResponseEntity.ok(lessonService.createAdminLesson(request));
    }

    /**
     * Update general information of the lesson.
     * 
     * @param id Lesson ID
     * @param request Update lesson request
     * @return Lesson
     */
    @PutMapping("/{id}")
    public ResponseEntity<LessonResponse> updateLessonGeneralInformation(@PathVariable UUID id,
            @Valid @RequestBody UpdateLessonInfoRequest request) {
        return ResponseEntity.ok(lessonService.updateGeneralInformation(id, request));
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
     * Analyze and classify categories for a specific lesson using AI.
     *
     * @param id Lesson ID
     * @return Result of categorized lesson
     */
    @PostMapping("/{id}/analyze-categories")
    public ResponseEntity<LessonCategoryAnalysisResponse> analyzeCategories(@Valid @NotNull @PathVariable UUID id) {
        return ResponseEntity.ok(lessonCategoryClassificationService.classifyLessonCategories(id));
    }

    /**
     * Generate missing Vietnamese translations for a lesson managed by admin.
     *
     * @param id Lesson ID
     * @return Current translation summary
     */
    @PostMapping("/{id}/translations/generate")
    public ResponseEntity<TranslationSummaryResponse> generateTranslations(@PathVariable UUID id) {
        return ResponseEntity.accepted().body(lessonService.requestAdminTranslationGeneration(id));
    }

    /**
     * Generate or regenerate the Vietnamese translation for one segment.
     *
     * @param id Lesson ID
     * @param segmentId Segment ID
     * @return Generated translation
     */
    @PostMapping("/{id}/segments/{segmentId}/translation/generate")
    public ResponseEntity<SegmentTranslationResponse> generateSegmentTranslation(@PathVariable UUID id,
            @PathVariable UUID segmentId) {
        return ResponseEntity.ok(lessonService.requestAdminSegmentTranslationGeneration(id, segmentId));
    }

    /**
     * Update lesson segments.
     * 
     * @param id Lesson ID
     * @param request Update lesson segments request
     * @return Lesson
     */
    @PutMapping("/{id}/segments")
    public ResponseEntity<LessonResponse> updateLessonSegments(@PathVariable UUID id,
            @Valid @RequestBody UpdateLessonSegmentsRequest request) {
        return ResponseEntity
                .ok(lessonService.updateLessonSegments(id, request.getSegments(), request.getDeletedSegmentIds()));

    }

    /**
     * Update lesson status.
     * 
     * @param id Lesson ID
     * @param request Update lesson status request
     * @return Lesson
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<LessonResponse> updateLessonStatus(@PathVariable UUID id,
            @Valid @RequestBody UpdateLessonStatusRequest request) {
        return ResponseEntity.ok(lessonService.updateLessonVisibility(id, request.getStatus()));
    }
}
