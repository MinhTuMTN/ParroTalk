package com.parrotalk.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "sense_antonyms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE sense_antonyms SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class SenseAntonym extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sense_id", nullable = false)
    private VocabularySense sense;

    @Column(name = "antonym_word", nullable = false)
    private String antonymWord;

    @Column(name = "antonym_vn")
    private String antonymVn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_vocabulary_id")
    private Vocabulary linkedVocabulary;
}
