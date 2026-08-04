package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.PracticeQuestionType;
import com.parrotalk.backend.constant.Sm2Rating;
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
 * Answer to a question within a practice session.
 */
@Entity
@Table(name = "practice_answers", indexes = {
        @Index(name = "idx_practice_answers_session", columnList = "session_id"),
        @Index(name = "idx_practice_answers_vocab", columnList = "user_vocabulary_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE practice_answers SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PracticeAnswer extends BaseEntity {

    /** Answer ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The session this answer belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    /** The vocabulary practiced. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_vocabulary_id", nullable = false)
    private UserVocabulary userVocabulary;

    /** Question type. */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private PracticeQuestionType questionType;

    /** If the user got the answer correct. */
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    /** The string user submitted (optional, used for typing mode). */
    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    /** SM-2 rating if applicable. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sm2Rating rating;

    /** Time the answer was submitted. */
    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    /** Time spent answering in ms. */
    @Column(name = "time_spent_ms")
    private Long timeSpentMs;
}
