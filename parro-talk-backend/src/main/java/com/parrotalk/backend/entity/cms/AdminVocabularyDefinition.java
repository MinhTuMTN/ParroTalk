package com.parrotalk.backend.entity.cms;

import com.parrotalk.backend.entity.BaseEntity;
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

@Entity
@Table(name = "admin_vocabulary_definitions", indexes = {
        @Index(name = "idx_admin_vocab_def_vocab_id", columnList = "vocabulary_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE admin_vocabulary_definitions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class AdminVocabularyDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private AdminVocabulary vocabulary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(name = "english_definition", columnDefinition = "TEXT")
    private String englishDefinition;

    @Column(name = "vietnamese_definition", columnDefinition = "TEXT")
    private String vietnameseDefinition;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
