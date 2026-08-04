package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.PracticeSessionStatus;
import jakarta.persistence.Column;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vocabulary practice session tracking entity.
 */
@Entity
@Table(name = "practice_sessions", indexes = {
        @Index(name = "idx_practice_sessions_user", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE practice_sessions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PracticeSession extends BaseEntity {

    /** Session ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Owner user. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** When the session started. */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** When the session completed. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Total questions generated for this session. */
    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private Integer totalQuestions = 0;

    /** Questions answered correctly. */
    @Column(name = "correct_answers", nullable = false)
    @Builder.Default
    private Integer correctAnswers = 0;

    /** Current session status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PracticeSessionStatus status = PracticeSessionStatus.IN_PROGRESS;

    /** Total XP earned during this session. */
    @Column(name = "xp_earned", nullable = false)
    @Builder.Default
    private Integer xpEarned = 0;
}
