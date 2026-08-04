package com.parrotalk.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.report.CreateLessonReportRequest;
import com.parrotalk.backend.dto.report.LessonReportResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.LessonReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Lesson Report Controller.
 *
 * <p>
 * A single {@code POST} endpoint serves both report kinds; the request body
 * discriminates them. Splitting into {@code /file-reports} and
 * {@code /translation-reports} would duplicate the lesson lookup, the
 * duplicate detection and the response mapping for no gain, since the two
 * payloads differ by one optional field.
 * </p>
 *
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/lessons/{lessonId}/reports")
@RequiredArgsConstructor
@Validated
public class LessonReportController {

    /** Lesson Report Service */
    private final LessonReportService lessonReportService;

    /**
     * Submit a report on a lesson.
     *
     * @param lessonId Lesson ID
     * @param request  Report payload
     * @param user     Authenticated user
     * @return Created report
     */
    @PostMapping
    public ResponseEntity<LessonReportResponse> createReport(
            @PathVariable UUID lessonId,
            @Valid @RequestBody CreateLessonReportRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lessonReportService.create(user, lessonId, request));
    }

    /**
     * List the reports the caller submitted on this lesson.
     *
     * @param lessonId Lesson ID
     * @param page     Page index
     * @param size     Page size
     * @param user     Authenticated user
     * @return Page of own reports
     */
    @GetMapping
    public ResponseEntity<PageResponse<LessonReportResponse>> listMyReports(
            @PathVariable UUID lessonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonReportService.listOwnReports(user, lessonId, page, size));
    }
}
