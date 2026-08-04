package com.parrotalk.backend.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.parrotalk.backend.constant.LessonReportType;
import com.parrotalk.backend.constant.ModerationPriority;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.entity.LessonReport;

/**
 * Lesson Report Specification.
 *
 * <p>
 * Every factory returns a no-op predicate for a {@code null} argument so the
 * admin filter can be composed unconditionally.
 * </p>
 *
 * @author MinhTuMTN
 */
public class LessonReportSpecification {

    private LessonReportSpecification() {
    }

    /**
     * Filter by triage status.
     *
     * @param status Status, may be {@code null}
     * @return Specification
     */
    public static Specification<LessonReport> hasStatus(ModerationStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("moderationState").get("status"), status);
    }

    /**
     * Filter by report kind.
     *
     * @param reportType Report type, may be {@code null}
     * @return Specification
     */
    public static Specification<LessonReport> hasReportType(LessonReportType reportType) {
        return (root, query, cb) -> reportType == null
                ? cb.conjunction()
                : cb.equal(root.get("reportType"), reportType);
    }

    /**
     * Filter by triage priority.
     *
     * @param priority Priority, may be {@code null}
     * @return Specification
     */
    public static Specification<LessonReport> hasPriority(ModerationPriority priority) {
        return (root, query, cb) -> priority == null
                ? cb.conjunction()
                : cb.equal(root.get("moderationState").get("priority"), priority);
    }

    /**
     * Filter by reported lesson.
     *
     * @param lessonId Lesson id, may be {@code null}
     * @return Specification
     */
    public static Specification<LessonReport> hasLesson(UUID lessonId) {
        return (root, query, cb) -> lessonId == null
                ? cb.conjunction()
                : cb.equal(root.get("lesson").get("id"), lessonId);
    }

    /**
     * Filter by assigned admin.
     *
     * @param assigneeId Assignee id, may be {@code null}
     * @return Specification
     */
    public static Specification<LessonReport> hasAssignee(UUID assigneeId) {
        return (root, query, cb) -> assigneeId == null
                ? cb.conjunction()
                : cb.equal(root.get("moderationState").get("assignee").get("id"), assigneeId);
    }

    /**
     * Filter by reporter.
     *
     * @param reporterId Reporter id, may be {@code null}
     * @return Specification
     */
    public static Specification<LessonReport> hasReporter(UUID reporterId) {
        return (root, query, cb) -> reporterId == null
                ? cb.conjunction()
                : cb.equal(root.get("reporter").get("id"), reporterId);
    }
}
