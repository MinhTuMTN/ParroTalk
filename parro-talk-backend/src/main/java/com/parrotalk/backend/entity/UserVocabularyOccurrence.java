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
 * Concrete lesson/segment occurrence of a saved vocabulary item.
 */
@Entity
@Table(name = "user_vocabulary_occurrences", indexes = {
        @Index(name = "idx_vocab_occurrences_vocab", columnList = "user_vocabulary_id"),
        @Index(name = "idx_vocab_occurrences_lesson_segment", columnList = "lesson_id, segment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user_vocabulary_occurrences SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class UserVocabularyOccurrence extends BaseEntity {

    /** Occurrence ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Saved vocabulary item. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_vocabulary_id", nullable = false)
    private UserVocabulary userVocabulary;

    /** Lesson where this occurrence appears. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    /** Segment where this occurrence appears. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private TranscriptionSegment segment;

    /** Word surface form in the segment. */
    @Column(nullable = false)
    private String word;

    /** Word start time in seconds, if word-level timing is available. */
    @Column(name = "start_time")
    private Double startTime;

    /** Word end time in seconds, if word-level timing is available. */
    @Column(name = "end_time")
    private Double endTime;

    /** Segment text or cropped context around the word. */
    @Column(name = "context_text", columnDefinition = "TEXT")
    private String contextText;
}
