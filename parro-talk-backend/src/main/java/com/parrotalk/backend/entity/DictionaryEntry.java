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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shared dictionary cache entry.
 */
@Entity
@Table(name = "dictionary_entries", indexes = {
        @Index(name = "idx_dictionary_entries_normalized_word", columnList = "normalized_word")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_dictionary_entries_word_lang", columnNames = {"normalized_word", "language"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE dictionary_entries SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class DictionaryEntry extends BaseEntity {

    /** Dictionary entry ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Normalized lookup key. */
    @Column(name = "normalized_word", nullable = false)
    private String normalizedWord;

    /** Original or preferred display form. */
    @Column(name = "display_word", nullable = false)
    private String displayWord;

    /** Source language. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String language = "en";

    /** Main part of speech if available. */
    @Column(name = "part_of_speech", length = 100)
    private String partOfSpeech;

    /** Phonetic pronunciation. */
    @Column(length = 255)
    private String phonetic;

    /** CEFR Level (e.g., A1, B2). */
    @Column(name = "cefr_level", length = 10)
    private String cefrLevel;

    /** Audio URL for UK pronunciation. */
    @Column(name = "audio_uk_url", length = 500)
    private String audioUkUrl;

    /** Audio URL for US pronunciation. */
    @Column(name = "audio_us_url", length = 500)
    private String audioUsUrl;

    /** Short common meaning in Vietnamese. */
    @Column(name = "common_meaning_vi", length = 500)
    private String commonMeaningVi;

    /** Dictionary definitions stored as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definitions_json", nullable = false, columnDefinition = "jsonb")
    private String definitionsJson;

    /** Example sentences stored as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "examples_json", columnDefinition = "jsonb")
    private String examplesJson;

    /** Synonyms stored as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "synonyms_json", columnDefinition = "jsonb")
    private String synonymsJson;

    /** Antonyms stored as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "antonyms_json", columnDefinition = "jsonb")
    private String antonymsJson;

    /** Cache source/provider name. */
    @Column(length = 100)
    private String source;

    /** Last cache access time. */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
}
