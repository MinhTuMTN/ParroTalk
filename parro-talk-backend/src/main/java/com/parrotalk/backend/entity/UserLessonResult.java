package com.parrotalk.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * User Lesson Result Entity.
 * 
 * @author MinhTuMTN
 */
@Entity
@Table(name = "user_lesson_results", indexes = {
        @Index(name = "idx_user_lesson_results_user_lesson", columnList = "user_id, lesson_id"),
        @Index(name = "idx_user_lesson_results_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_lesson_results_user_lesson", columnNames = { "user_id", "lesson_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user_lesson_results SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class UserLessonResult extends BaseEntity {

    /** User Lesson Result ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** User ID */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Lesson ID */
    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    /** Score */
    @Column(nullable = false)
    private Double score;

    /** Hint words count */
    @Column(name = "hint_words", nullable = false)
    private Integer hintWords;

    /** Replay count */
    @Column(name = "replay_count", nullable = false)
    private Integer replayCount;

    /** Attempts count */
    @Column(nullable = false)
    private Integer attempts;

    /** Is passed */
    @Column(name = "is_passed", nullable = false)
    private Boolean isPassed;
}
