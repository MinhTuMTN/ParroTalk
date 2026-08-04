package com.parrotalk.backend.entity;

import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.parrotalk.backend.constant.LessonReportReason;
import com.parrotalk.backend.constant.LessonReportType;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Problem reported by a learner about the data of a lesson.
 *
 * <p>
 * A single table holds both report kinds, discriminated by
 * {@link LessonReportType}. {@code segment_id} is nullable at the column level
 * but guarded by database CHECK constraints: mandatory for
 * {@link LessonReportType#TRANSLATION}, forbidden for
 * {@link LessonReportType#FILE}.
 * </p>
 *
 * @author MinhTuMTN
 */
@Entity
@Table(name = "lesson_reports", indexes = {
        @Index(name = "idx_lesson_reports_lesson_created_at", columnList = "lesson_id, created_at"),
        @Index(name = "idx_lesson_reports_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_lesson_reports_reporter_created_at", columnList = "reporter_id, created_at"),
        @Index(name = "idx_lesson_reports_assignee_status", columnList = "assignee_id, status"),
        @Index(name = "idx_lesson_reports_segment_id", columnList = "segment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE lesson_reports SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class LessonReport extends BaseEntity {

    /** Lesson report ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Lesson the report belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    /** Reported segment, only for {@link LessonReportType#TRANSLATION}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private TranscriptionSegment segment;

    /** Which kind of lesson data is reported. */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private LessonReportType reportType;

    /** Reason picked by the reporter. */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private LessonReportReason reason;

    /** Optional free-text detail written by the reporter. */
    @Column(name = "description", length = 1000)
    private String description;

    /** User who submitted the report. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /** Shared triage state. */
    @Embedded
    @Builder.Default
    private ModerationState moderationState = ModerationState.initial();
}
