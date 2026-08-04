package com.parrotalk.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.constant.LessonReportType;
import com.parrotalk.backend.constant.ModerationPriority;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.moderation.ModerationEventResponse;
import com.parrotalk.backend.dto.moderation.ModerationUpdateRequest;
import com.parrotalk.backend.dto.report.LessonReportResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.LessonReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin Lesson Report Controller.
 *
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/admin/lesson-reports")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminLessonReportController {

    /** Lesson Report Service */
    private final LessonReportService lessonReportService;

    /**
     * Search reports.
     *
     * @param status     Triage status filter
     * @param type       Report kind filter
     * @param priority   Priority filter
     * @param lessonId   Lesson filter
     * @param assigneeId Assignee filter
     * @param page       Page index
     * @param size       Page size
     * @return Page of reports
     */
    @GetMapping
    public ResponseEntity<PageResponse<LessonReportResponse>> getReports(
            @RequestParam(required = false) ModerationStatus status,
            @RequestParam(required = false) LessonReportType type,
            @RequestParam(required = false) ModerationPriority priority,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                lessonReportService.search(status, type, priority, lessonId, assigneeId, page, size));
    }

    /**
     * Get report detail.
     *
     * @param id Report ID
     * @return Report detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<LessonReportResponse> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonReportService.getDetail(id));
    }

    /**
     * Update the triage state of a report.
     *
     * @param id      Report ID
     * @param request Requested changes
     * @param admin   Authenticated admin
     * @return Updated report
     */
    @PatchMapping("/{id}")
    public ResponseEntity<LessonReportResponse> updateReport(
            @PathVariable UUID id,
            @Valid @RequestBody ModerationUpdateRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(lessonReportService.updateModeration(id, request, admin));
    }

    /**
     * Get the audit trail of a report.
     *
     * @param id Report ID
     * @return Ordered audit entries
     */
    @GetMapping("/{id}/events")
    public ResponseEntity<List<ModerationEventResponse>> getReportEvents(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonReportService.getEvents(id));
    }
}
