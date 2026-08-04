package com.parrotalk.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.constant.LessonReportReason;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.entity.LessonReport;

@Repository
public interface LessonReportRepository
        extends JpaRepository<LessonReport, UUID>, JpaSpecificationExecutor<LessonReport> {

    /**
     * Admin listing. Associations are fetched eagerly to avoid N+1 while
     * mapping the page to responses.
     *
     * @param specification Filter built by {@code LessonReportSpecification}
     * @param pageable      Paging and sorting
     * @return Page of reports
     */
    @Override
    @EntityGraph(attributePaths = { "reporter", "lesson", "segment", "moderationState.assignee" })
    Page<LessonReport> findAll(Specification<LessonReport> specification, Pageable pageable);

    /**
     * Reports submitted by a user on a given lesson.
     *
     * @param reporterId Reporter id
     * @param lessonId   Lesson id
     * @param pageable   Paging and sorting
     * @return Page of reports
     */
    @EntityGraph(attributePaths = { "reporter", "lesson", "segment", "moderationState.assignee" })
    Page<LessonReport> findByReporterIdAndLessonId(UUID reporterId, UUID lessonId, Pageable pageable);

    /**
     * Find still-open reports the same user already filed on the same lesson
     * with the same reason. Used to collapse accidental double submissions.
     *
     * <p>
     * The segment is compared in the service layer so the query stays free of
     * nullable-parameter tricks.
     * </p>
     *
     * @param reporterId Reporter id
     * @param lessonId   Lesson id
     * @param reason     Reported reason
     * @param statuses   Statuses considered still active
     * @return Matching reports
     */
    @Query("""
            select r from LessonReport r
            where r.reporter.id = :reporterId
              and r.lesson.id = :lessonId
              and r.reason = :reason
              and r.moderationState.status in :statuses
            """)
    List<LessonReport> findActiveByReporterAndLessonAndReason(
            @Param("reporterId") UUID reporterId,
            @Param("lessonId") UUID lessonId,
            @Param("reason") LessonReportReason reason,
            @Param("statuses") Collection<ModerationStatus> statuses);
}
