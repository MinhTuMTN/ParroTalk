package com.parrotalk.backend.dto.report;

import java.time.LocalDateTime;
import java.util.UUID;

import com.parrotalk.backend.constant.LessonReportReason;
import com.parrotalk.backend.constant.LessonReportType;
import com.parrotalk.backend.dto.moderation.ModerationStateResponse;
import com.parrotalk.backend.dto.moderation.UserSummaryResponse;
import com.parrotalk.backend.entity.LessonReport;

/**
 * Lesson report as returned to its reporter and to admins.
 *
 * @param id          Report id
 * @param lessonId    Reported lesson
 * @param segmentId   Reported segment, {@code null} for file reports
 * @param type        Report kind
 * @param reason      Reason picked by the reporter
 * @param description Free-text detail
 * @param reporter    Who submitted the report
 * @param moderation  Triage state
 * @param createdAt   Submission time
 */
public record LessonReportResponse(
        UUID id,
        UUID lessonId,
        UUID segmentId,
        LessonReportType type,
        LessonReportReason reason,
        String description,
        UserSummaryResponse reporter,
        ModerationStateResponse moderation,
        LocalDateTime createdAt) {

    /**
     * Map a lesson report entity.
     *
     * @param report Lesson report
     * @return Response projection
     */
    public static LessonReportResponse from(LessonReport report) {
        return new LessonReportResponse(
                report.getId(),
                report.getLesson().getId(),
                report.getSegment() == null ? null : report.getSegment().getId(),
                report.getReportType(),
                report.getReason(),
                report.getDescription(),
                UserSummaryResponse.from(report.getReporter()),
                ModerationStateResponse.from(report.getModerationState()),
                report.getCreatedAt());
    }
}
