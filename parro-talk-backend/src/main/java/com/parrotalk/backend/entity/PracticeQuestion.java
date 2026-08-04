package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.PracticeQuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

import java.util.UUID;

/**
 * Stores a generated practice question so the session can be resumed.
 */
@Entity
@Table(name = "practice_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE practice_questions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PracticeQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_vocabulary_id", nullable = false)
    private UserVocabulary userVocabulary;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private PracticeQuestionType questionType;

    /** 
     * JSON string array of options for multiple choice questions.
     * Stored as a string to simplify mapping and persist exactly what the user sees.
     */
    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;
}
