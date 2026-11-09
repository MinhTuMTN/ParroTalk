package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.VocabularyCrawlStatus;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "vocabulary_list_items", indexes = {
        @Index(name = "idx_vocabulary_list_items_list", columnList = "list_id"),
        @Index(name = "idx_vocabulary_list_items_vocab", columnList = "vocabulary_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_vocab_list_items_list_word", columnNames = {"list_id", "normalized_word"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE vocabulary_list_items SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class VocabularyListItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private VocabularyList vocabularyList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    private Vocabulary vocabulary;

    @Column(name = "raw_word", nullable = false, length = 255)
    private String rawWord;

    @Column(name = "normalized_word", nullable = false, length = 255)
    private String normalizedWord;

    @Column(name = "source_option", nullable = false, length = 30)
    @Builder.Default
    private String sourceOption = "DB_EXISTING";

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_status", nullable = false, length = 30)
    @Builder.Default
    private VocabularyCrawlStatus crawlStatus = VocabularyCrawlStatus.COMPLETED;

    @Column(name = "crawl_error_message", columnDefinition = "TEXT")
    private String crawlErrorMessage;

    @Column(name = "user_note", columnDefinition = "TEXT")
    private String userNote;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "selected_sense_ids_json", columnDefinition = "TEXT")
    private String selectedSenseIdsJson;

    @Column(name = "selected_idiom_ids_json", columnDefinition = "TEXT")
    private String selectedIdiomIdsJson;

    @Column(name = "selected_phrasal_verb_ids_json", columnDefinition = "TEXT")
    private String selectedPhrasalVerbIdsJson;

    public List<UUID> getSelectedSenseIds() {
        return parseUuidJsonArray(selectedSenseIdsJson);
    }

    public void setSelectedSenseIds(List<UUID> ids) {
        this.selectedSenseIdsJson = toUuidJsonArray(ids);
    }

    public List<UUID> getSelectedIdiomIds() {
        return parseUuidJsonArray(selectedIdiomIdsJson);
    }

    public void setSelectedIdiomIds(List<UUID> ids) {
        this.selectedIdiomIdsJson = toUuidJsonArray(ids);
    }

    public List<UUID> getSelectedPhrasalVerbIds() {
        return parseUuidJsonArray(selectedPhrasalVerbIdsJson);
    }

    public void setSelectedPhrasalVerbIds(List<UUID> ids) {
        this.selectedPhrasalVerbIdsJson = toUuidJsonArray(ids);
    }

    private static List<UUID> parseUuidJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return Collections.emptyList();
        }
        List<UUID> result = new ArrayList<>();
        for (String token : Arrays.asList(trimmed.split(","))) {
            String clean = token.trim().replace("\"", "");
            if (!clean.isEmpty()) {
                try {
                    result.add(UUID.fromString(clean));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }

    private static String toUuidJsonArray(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        return "[" + ids.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(id -> "\"" + id.toString() + "\"")
                .collect(Collectors.joining(",")) + "]";
    }
}

