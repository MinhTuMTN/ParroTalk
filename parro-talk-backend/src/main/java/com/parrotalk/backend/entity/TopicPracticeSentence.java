package com.parrotalk.backend.entity;

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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Entity representing context-rich practice sentences with blanks and grammar explanations,
 * generated for vocabulary words within specific IELTS topics.
 */
@Entity
@Table(name = "topic_practice_sentences", indexes = {
        @Index(name = "idx_topic_practice_sentences_topic_vocab", columnList = "topic_id, vocabulary_id"),
        @Index(name = "idx_topic_practice_sentences_target_word", columnList = "target_word")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE topic_practice_sentences SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TopicPracticeSentence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private Vocabulary vocabulary;

    @Column(name = "target_word", nullable = false)
    private String targetWord;

    @Column(name = "target_form", nullable = false)
    private String targetForm;

    @Column(name = "sentence_with_blank", nullable = false, columnDefinition = "TEXT")
    private String sentenceWithBlank;

    @Column(name = "completed_sentence", nullable = false, columnDefinition = "TEXT")
    private String completedSentence;

    @Column(name = "sentence_vn", columnDefinition = "TEXT")
    private String sentenceVn;

    @Column(name = "grammar_context_tip", columnDefinition = "TEXT")
    private String grammarContextTip;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @jakarta.persistence.Transient
    @Builder.Default
    private boolean fallback = false;
}
