package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.WritingItemType;
import com.parrotalk.backend.constant.WritingItemValidationStatus;
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

import java.util.UUID;

@Entity
@Table(name = "writing_generated_items", indexes = {
        @Index(name = "idx_writing_items_session", columnList = "session_id, display_order"),
        @Index(name = "idx_writing_items_validation_status", columnList = "validation_status"),
        @Index(name = "idx_writing_items_linked_vocab", columnList = "linked_vocabulary_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE writing_generated_items SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class WritingGeneratedItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private WritingSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 32)
    private WritingItemType itemType;

    @Column(name = "content_en", nullable = false, columnDefinition = "TEXT")
    private String contentEn;

    @Column(name = "normalized_text", length = 500)
    private String normalizedText;

    @Column(name = "phonetic", length = 255)
    private String phonetic;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "part_of_speech", length = 64)
    private String partOfSpeech;

    @Column(name = "cefr_level", length = 16)
    private String cefrLevel;

    @Column(name = "meaning_vn", columnDefinition = "TEXT")
    private String meaningVn;

    @Column(name = "explanation_vn", columnDefinition = "TEXT")
    private String explanationVn;

    @Column(name = "usage_context_vn", columnDefinition = "TEXT")
    private String usageContextVn;

    @Column(name = "example_en", columnDefinition = "TEXT")
    private String exampleEn;

    @Column(name = "example_vn", columnDefinition = "TEXT")
    private String exampleVn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_vocabulary_id")
    private Vocabulary linkedVocabulary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_sense_id")
    private VocabularySense linkedSense;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 32)
    @Builder.Default
    private WritingItemValidationStatus validationStatus = WritingItemValidationStatus.PENDING;

    @Column(name = "validation_notes", columnDefinition = "TEXT")
    private String validationNotes;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}

