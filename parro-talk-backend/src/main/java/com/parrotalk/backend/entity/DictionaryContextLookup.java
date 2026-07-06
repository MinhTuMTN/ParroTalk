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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cached word meaning for a specific sentence/context.
 */
@Entity
@Table(name = "dictionary_context_lookups", indexes = {
        @Index(name = "idx_dictionary_context_word", columnList = "normalized_word")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_dictionary_context_word_hash", columnNames = {"normalized_word", "context_hash"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE dictionary_context_lookups SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class DictionaryContextLookup extends BaseEntity {

    /** Context lookup ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Normalized lookup key. */
    @Column(name = "normalized_word", nullable = false)
    private String normalizedWord;

    /** Word as selected or sent by the client. */
    @Column(name = "original_word", nullable = false)
    private String originalWord;

    /** SHA-256 hash of normalized word and normalized context. */
    @Column(name = "context_hash", nullable = false, length = 64)
    private String contextHash;

    /** Context sentence or cropped context around the word. */
    @Column(name = "context_text", nullable = false, columnDefinition = "TEXT")
    private String contextText;

    /** Contextual Vietnamese meaning. */
    @Column(name = "meaning_vi", nullable = false, columnDefinition = "TEXT")
    private String meaningVi;

    /** Short Vietnamese meaning for compact UI. */
    @Column(name = "short_meaning_vi", length = 500)
    private String shortMeaningVi;

    /** Vietnamese explanation for why this meaning fits the context. */
    @Column(name = "explanation_vi", columnDefinition = "TEXT")
    private String explanationVi;

    /** Part of speech in this context. */
    @Column(name = "part_of_speech", length = 100)
    private String partOfSpeech;

    /** LLM/provider confidence between 0 and 1. */
    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    /** LLM/provider name. */
    @Column(length = 100)
    private String provider;

    /** Model name used for the lookup. */
    @Column(length = 255)
    private String model;
}
