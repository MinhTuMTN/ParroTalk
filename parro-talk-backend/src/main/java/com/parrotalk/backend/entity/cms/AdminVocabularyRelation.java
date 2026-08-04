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
@Table(name = "admin_vocabulary_relations", indexes = {
        @Index(name = "idx_admin_vocab_rel_vocab_id", columnList = "vocabulary_id, relation_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE admin_vocabulary_relations SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class AdminVocabularyRelation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private AdminVocabulary vocabulary;

    /** Type: SYNONYM, ANTONYM, COLLOCATION, IDIOM, PHRASAL_VERB, WORD_FORM */
    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;

    @Column(name = "related_word", nullable = false, length = 255)
    private String relatedWord;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
