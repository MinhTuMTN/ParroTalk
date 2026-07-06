package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.VocabularyDifficulty;
import com.parrotalk.backend.constant.VocabularyStatus;
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
import jakarta.persistence.OneToMany;
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
import java.util.List;
import java.util.UUID;

/**
 * Vocabulary item saved by a user.
 */
@Entity
@Table(name = "user_vocabularies", indexes = {
        @Index(name = "idx_user_vocabularies_user_status", columnList = "user_id, status"),
        @Index(name = "idx_user_vocabularies_next_review", columnList = "user_id, next_review_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_vocabularies_user_word", columnNames = {"user_id", "normalized_word"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user_vocabularies SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class UserVocabulary extends BaseEntity {

    /** User vocabulary ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Owner user. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Normalized lookup key. */
    @Column(name = "normalized_word", nullable = false)
    private String normalizedWord;

    /** Display word as the user saw it. */
    @Column(name = "display_word", nullable = false)
    private String displayWord;

    /** Optional shared dictionary entry. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dictionary_entry_id")
    private DictionaryEntry dictionaryEntry;

    /** User note. */
    @Column(columnDefinition = "TEXT")
    private String note;

    /** Learning status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private VocabularyStatus status = VocabularyStatus.NEW;

    /** User-defined difficulty. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VocabularyDifficulty difficulty;

    /** Review count for future spaced repetition. */
    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    /** Last review time. */
    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    /** Next scheduled review time. */
    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    /** Lesson/segment occurrences where this word appears. */
    @OneToMany(mappedBy = "userVocabulary", fetch = FetchType.LAZY)
    private List<UserVocabularyOccurrence> occurrences;
}
