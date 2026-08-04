package com.parrotalk.backend.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.LessonReportType;
import com.parrotalk.backend.constant.ModerationPriority;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.constant.ModerationTargetType;
import com.parrotalk.backend.util.PageableUtils;
import org.springframework.data.domain.Sort;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.moderation.ModerationEventResponse;
import com.parrotalk.backend.dto.moderation.ModerationUpdateRequest;
import com.parrotalk.backend.dto.report.CreateLessonReportRequest;
import com.parrotalk.backend.dto.report.LessonReportResponse;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.LessonReport;
import com.parrotalk.backend.entity.ModerationState;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.LessonReportRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import com.parrotalk.backend.specification.LessonReportSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lesson data problem reports submitted by learners.
 *
 * <p>
 * Owns only the lesson-specific rules; the triage workflow lives in
 * {@link ModerationService}.
 * </p>
 *
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LessonReportService {

    private static final ModerationTargetType TARGET_TYPE = ModerationTargetType.LESSON_REPORT;

    private final LessonReportRepository lessonReportRepository;
    private final LessonRepository lessonRepository;
    private final TranscriptionSegmentRepository transcriptionSegmentRepository;
    private final ModerationService moderationService;

    /**
     * Submit a report on a lesson.
     *
     * <p>
     * Re-submitting the same reason on the same target while a previous report
     * is still open returns the existing report instead of creating a
     * duplicate.
     * </p>
     *
     * @param reporter Authenticated reporter
     * @param lessonId Reported lesson
     * @param request  Report payload
     * @return Created or already existing report
     */
    @Transactional
    public LessonReportResponse create(User reporter, UUID lessonId, CreateLessonReportRequest request) {
        Lesson lesson = findLesson(lessonId);
        validateReason(request);
        TranscriptionSegment segment = resolveSegment(lesson, request);

        LessonReport existing = findActiveDuplicate(reporter.getId(), lessonId, request, segment);
        if (existing != null) {
            log.info("Duplicate lesson report ignored: reportId={}, lessonId={}, reporterId={}",
                    existing.getId(), lessonId, reporter.getId());
            return LessonReportResponse.from(existing);
        }

        LessonReport report = lessonReportRepository.save(LessonReport.builder()
                .lesson(lesson)
                .segment(segment)
                .reportType(request.type())
                .reason(request.reason())
                .description(normalize(request.description()))
                .reporter(reporter)
                .moderationState(ModerationState.initial())
                .build());

        moderationService.recordCreation(TARGET_TYPE, report.getId(), reporter);

        // Identifiers and enums only: the free-text description may contain
        // user-supplied content and is deliberately kept out of the logs.
        log.info("Lesson report created: reportId={}, lessonId={}, segmentId={}, type={}, reason={}, reporterId={}",
                report.getId(), lessonId, segment == null ? null : segment.getId(),
                report.getReportType(), report.getReason(), reporter.getId());

        return LessonReportResponse.from(report);
    }

    /**
     * List the reports the caller submitted on a lesson.
     *
     * @param reporter Authenticated reporter
     * @param lessonId Lesson id
     * @param page     Page index
     * @param size     Page size
     * @return Page of own reports
     */
    @Transactional(readOnly = true)
    public PageResponse<LessonReportResponse> listOwnReports(User reporter, UUID lessonId, int page, int size) {
        Pageable pageable = PageableUtils.createPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LessonReport> result = lessonReportRepository
                .findByReporterIdAndLessonId(reporter.getId(), lessonId, pageable);
        return toPageResponse(result);
    }

    /**
     * Admin listing with optional filters.
     *
     * @param status     Triage status filter
     * @param reportType Report kind filter
     * @param priority   Priority filter
     * @param lessonId   Lesson filter
     * @param assigneeId Assignee filter
     * @param page       Page index
     * @param size       Page size
     * @return Page of reports
     */
    @Transactional(readOnly = true)
    public PageResponse<LessonReportResponse> search(
            ModerationStatus status,
            LessonReportType reportType,
            ModerationPriority priority,
            UUID lessonId,
            UUID assigneeId,
            int page,
            int size) {

        Specification<LessonReport> specification = Specification
                .where(LessonReportSpecification.hasStatus(status))
                .and(LessonReportSpecification.hasReportType(reportType))
                .and(LessonReportSpecification.hasPriority(priority))
                .and(LessonReportSpecification.hasLesson(lessonId))
                .and(LessonReportSpecification.hasAssignee(assigneeId));

        return toPageResponse(lessonReportRepository.findAll(specification, PageableUtils.createPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    /**
     * Read a single report.
     *
     * @param reportId Report id
     * @return Report detail
     */
    @Transactional(readOnly = true)
    public LessonReportResponse getDetail(UUID reportId) {
        return LessonReportResponse.from(findReport(reportId));
    }

    /**
     * Update the triage state of a report.
     *
     * @param reportId Report id
     * @param request  Requested changes
     * @param actor    Admin performing the change
     * @return Updated report
     */
    @Transactional
    public LessonReportResponse updateModeration(UUID reportId, ModerationUpdateRequest request, User actor) {
        LessonReport report = findReport(reportId);
        moderationService.applyUpdate(report.getModerationState(), TARGET_TYPE, reportId, request, actor);
        return LessonReportResponse.from(lessonReportRepository.save(report));
    }

    /**
     * Read the audit trail of a report.
     *
     * @param reportId Report id
     * @return Ordered audit entries
     */
    @Transactional(readOnly = true)
    public List<ModerationEventResponse> getEvents(UUID reportId) {
        findReport(reportId);
        return moderationService.getEvents(TARGET_TYPE, reportId);
    }

    /**
     * Reject reasons that do not belong to the submitted report type.
     */
    private void validateReason(CreateLessonReportRequest request) {
        if (!request.reason().isValidFor(request.type())) {
            throw new ParroTalkException(
                    "Reason %s is not valid for report type %s.".formatted(request.reason(), request.type()),
                    "REPORT_REASON_TYPE_MISMATCH",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Resolve the reported segment and enforce the type-specific rules:
     * mandatory and lesson-owned for TRANSLATION, absent for FILE.
     */
    private TranscriptionSegment resolveSegment(Lesson lesson, CreateLessonReportRequest request) {
        if (request.type() == LessonReportType.FILE) {
            if (request.segmentId() != null) {
                throw new ParroTalkException(
                        "A file report must not reference a segment.",
                        "REPORT_SEGMENT_NOT_ALLOWED",
                        HttpStatus.BAD_REQUEST);
            }
            return null;
        }

        if (request.segmentId() == null) {
            throw new ParroTalkException(
                    "A translation report must reference a segment.",
                    "REPORT_SEGMENT_REQUIRED",
                    HttpStatus.BAD_REQUEST);
        }

        TranscriptionSegment segment = transcriptionSegmentRepository.findById(request.segmentId())
                .orElseThrow(() -> new ParroTalkException(
                        "Segment not found.", "SEGMENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!Objects.equals(segment.getLesson().getId(), lesson.getId())) {
            throw new ParroTalkException(
                    "Segment does not belong to lesson.",
                    "SEGMENT_NOT_IN_LESSON",
                    HttpStatus.BAD_REQUEST);
        }
        return segment;
    }

    /**
     * Best-effort collapse of accidental double submissions. A concurrent
     * duplicate is harmless, so no lock is taken.
     */
    private LessonReport findActiveDuplicate(
            UUID reporterId, UUID lessonId, CreateLessonReportRequest request, TranscriptionSegment segment) {

        UUID segmentId = segment == null ? null : segment.getId();
        return lessonReportRepository
                .findActiveByReporterAndLessonAndReason(
                        reporterId, lessonId, request.reason(), ModerationService.ACTIVE_STATUSES)
                .stream()
                .filter(report -> Objects.equals(
                        report.getSegment() == null ? null : report.getSegment().getId(), segmentId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolve the lesson without loading its segments and categories.
     *
     * <p>
     * {@code LessonRepository#findById} carries an entity graph that eagerly
     * fetches every segment, which is far too much for submitting a report.
     * Only the existence check and the foreign key are needed here.
     * </p>
     */
    private Lesson findLesson(UUID lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ParroTalkException("Lesson not found.", "LESSON_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        return lessonRepository.getReferenceById(lessonId);
    }

    private LessonReport findReport(UUID reportId) {
        return lessonReportRepository.findById(reportId)
                .orElseThrow(() -> new ParroTalkException(
                        "Lesson report not found.", "LESSON_REPORT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private String normalize(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PageResponse<LessonReportResponse> toPageResponse(Page<LessonReport> result) {
        return PageResponse.<LessonReportResponse>builder()
                .content(result.getContent().stream().map(LessonReportResponse::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }
}
