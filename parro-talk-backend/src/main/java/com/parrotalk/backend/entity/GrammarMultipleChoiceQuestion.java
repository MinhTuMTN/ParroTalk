package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.McqQuestionCategory;
import com.parrotalk.backend.constant.QuestionGenerationSource;
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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "grammar_multiple_choice_questions", indexes = {
        @Index(name = "idx_grammar_mcq_sense", columnList = "sense_id"),
        @Index(name = "idx_grammar_mcq_idiom", columnList = "idiom_id"),
        @Index(name = "idx_grammar_mcq_phrasal_verb", columnList = "phrasal_verb_id"),
        @Index(name = "idx_grammar_mcq_generation_source", columnList = "generation_source")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_grammar_mcq_content_hash", columnNames = {"content_hash"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE grammar_multiple_choice_questions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class GrammarMultipleChoiceQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sense_id")
    private VocabularySense sense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idiom_id")
    private Idiom idiom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phrasal_verb_id")
    private PhrasalVerb phrasalVerb;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    private Vocabulary vocabulary;

    @Column(name = "grammar_topic", nullable = false, length = 64)
    private String grammarTopic;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_category", nullable = false, length = 40)
    @Builder.Default
    private McqQuestionCategory questionCategory = McqQuestionCategory.GRAMMAR_PATTERN;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_source", nullable = false, length = 40)
    @Builder.Default
    private QuestionGenerationSource generationSource = QuestionGenerationSource.PIPELINE_BATCH;

    @Column(name = "ai_provider_model", length = 100)
    private String aiProviderModel;

    @Column(name = "difficulty", nullable = false, length = 2)
    private String difficulty;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "correct_index", nullable = false)
    private Short correctIndex;

    @Column(name = "correct_answer", nullable = false, length = 255)
    private String correctAnswer;

    @Column(name = "completed_sentence", nullable = false, columnDefinition = "TEXT")
    private String completedSentence;

    @Column(name = "explanation_en", nullable = false, columnDefinition = "TEXT")
    private String explanationEn;

    @Column(name = "explanation_vn", columnDefinition = "TEXT")
    private String explanationVn;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "source_list_id")
    private UUID sourceListId;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;
}

