package com.parrotalk.backend.dto.report;

import java.util.UUID;

import com.parrotalk.backend.constant.LessonReportReason;
import com.parrotalk.backend.constant.LessonReportType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload of {@code POST /api/lessons/{lessonId}/reports}.
 *
 * <p>
 * One endpoint serves both report kinds; {@code type} discriminates them.
 * Structural rules that Bean Validation cannot express - reason must belong to
 * the type, segment is required for {@code TRANSLATION} and must belong to the
 * lesson - are enforced in the service layer.
 * </p>
 *
 * @param type        Report kind
 * @param reason      Reason picked by the user
 * @param segmentId   Reported segment, required for {@code TRANSLATION}
 * @param description Optional free-text detail
 */
public record CreateLessonReportRequest(
        @NotNull(message = "type is required") LessonReportType type,
        @NotNull(message = "reason is required") LessonReportReason reason,
        UUID segmentId,
        @Size(max = 1000, message = "description must not exceed 1000 characters") String description) {
}
