package com.parrotalk.backend.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * User lesson history entity.
 * 
 * @author MinhTuMTN
 */
@Entity
@Table(name = "user_lesson_history", indexes = {
        @Index(name = "idx_user_lesson_history_user", columnList = "user_id"),
        @Index(name = "idx_user_lesson_history_lesson", columnList = "lesson_id"),
        @Index(name = "idx_user_lesson_history_user_lesson_submitted", columnList = "user_id, lesson_id, submitted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLessonHistory extends BaseEntity {

    /** User lesson history ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID historyId;

    /** User */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Lesson */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    /** Overall score */
    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    /** Total time spent */
    @Column(name = "total_time_spent")
    private Integer totalTimeSpent;
}
