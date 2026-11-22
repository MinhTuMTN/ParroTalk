package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.QuizGradingStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_lesson_quiz_answers", indexes = {
        @Index(name = "idx_user_lesson_quiz_answers_attempt", columnList = "attempt_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_quiz_answer_attempt_question", columnNames = {"attempt_id", "question_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user_lesson_quiz_answers SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class UserLessonQuizAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private UserLessonQuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private LessonQuizQuestion question;

    @Column(name = "user_answer", nullable = false, columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "selected_index")
    private Short selectedIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", nullable = false, length = 32)
    private QuizGradingStatus gradingStatus;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "feedback_vn", columnDefinition = "TEXT")
    private String feedbackVn;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;
}
