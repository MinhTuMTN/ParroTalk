package com.parrotalk.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Normalized core Vocabulary entity.
 */
@Entity
@Table(name = "vocabularies", indexes = {
        @Index(name = "idx_vocabularies_normalized_word", columnList = "normalized_word")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_vocabularies_normalized_word", columnNames = {"normalized_word"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE vocabularies SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Vocabulary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String word;

    @Column(name = "normalized_word", nullable = false)
    private String normalizedWord;

    @Column(name = "common_meaning_vn", length = 500)
    private String commonMeaningVn;

    @OneToMany(mappedBy = "vocabulary", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<VocabularyPronunciation> pronunciations = new ArrayList<>();

    @OneToMany(mappedBy = "vocabulary", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<VocabularySense> senses = new ArrayList<>();

    @OneToMany(mappedBy = "vocabulary", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<Idiom> idioms = new ArrayList<>();

    @OneToMany(mappedBy = "vocabulary", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<PhrasalVerb> phrasalVerbs = new ArrayList<>();


}
