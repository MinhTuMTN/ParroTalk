package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.VocabularyDataSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "phrasal_verbs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE phrasal_verbs SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PhrasalVerb extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private Vocabulary vocabulary;

    @Column(nullable = false)
    private String phrase;

    @Column(columnDefinition = "TEXT")
    private String definition;

    @Column(name = "definition_vn", columnDefinition = "TEXT")
    private String definitionVn;

    @Column(columnDefinition = "TEXT")
    private String example;

    @Column(name = "example_vn", columnDefinition = "TEXT")
    private String exampleVn;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 40)
    @Builder.Default
    private VocabularyDataSource dataSource = VocabularyDataSource.IMPORT_JSON;
}
