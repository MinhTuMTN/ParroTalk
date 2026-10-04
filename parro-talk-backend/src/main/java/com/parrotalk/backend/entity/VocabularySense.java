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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vocabulary_senses", indexes = {
        @Index(name = "idx_vocabulary_senses_vocab", columnList = "vocabulary_id"),
        @Index(name = "idx_vocabulary_senses_topic", columnList = "topic_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE vocabulary_senses SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class VocabularySense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private Vocabulary vocabulary;

    @Column(name = "part_of_speech", length = 100)
    private String partOfSpeech;

    @Column(name = "cefr_level", length = 10)
    private String cefrLevel;

    @Column(name = "is_ai_cefr", nullable = false)
    @Builder.Default
    private Boolean isAiCefr = false;

    @Column(name = "definition_en", columnDefinition = "TEXT")
    private String definitionEn;

    @Column(name = "definition_vn", columnDefinition = "TEXT")
    private String definitionVn;

    @Column(name = "example_en", columnDefinition = "TEXT")
    private String exampleEn;

    @Column(name = "example_vn", columnDefinition = "TEXT")
    private String exampleVn;

    @OneToMany(mappedBy = "sense", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<SenseSynonym> synonyms = new ArrayList<>();

    @OneToMany(mappedBy = "sense", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<SenseAntonym> antonyms = new ArrayList<>();

    @OneToMany(mappedBy = "sense", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<SenseCollocation> collocations = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    // ---- backward-compatibility helpers ----
    // Keep getTopics()/setTopics() so existing service/test code compiles without mass edits.
    public List<Topic> getTopics() {
        return topic != null ? List.of(topic) : Collections.emptyList();
    }

    public void setTopics(List<Topic> topics) {
        this.topic = (topics != null && !topics.isEmpty()) ? topics.get(0) : null;
    }
}
