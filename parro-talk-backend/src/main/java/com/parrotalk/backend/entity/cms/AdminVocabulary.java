package com.parrotalk.backend.entity.cms;

import com.parrotalk.backend.entity.BaseEntity;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Tag;
import com.parrotalk.backend.entity.User;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "admin_vocabularies", indexes = {
        @Index(name = "idx_admin_vocabularies_word", columnList = "word"),
        @Index(name = "idx_admin_vocabularies_cefr", columnList = "cefr_level")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE admin_vocabularies SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class AdminVocabulary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String word;

    @Column(name = "ipa_uk", length = 255)
    private String ipaUk;

    @Column(name = "ipa_us", length = 255)
    private String ipaUs;

    @Column(name = "audio_uk", length = 500)
    private String audioUk;

    @Column(name = "audio_us", length = 500)
    private String audioUs;

    @Column(name = "cefr_level", length = 10)
    private String cefrLevel;

    @Column(name = "frequency_rank")
    private Integer frequencyRank;

    @Column(name = "part_of_speech", length = 100)
    private String partOfSpeech;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 255)
    private String source;

    @Column(length = 50)
    @Builder.Default
    private String status = "DRAFT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @OneToMany(mappedBy = "vocabulary", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<AdminVocabularyDefinition> definitions;

    @OneToMany(mappedBy = "vocabulary", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<AdminVocabularyExample> examples;

    @OneToMany(mappedBy = "vocabulary", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<AdminVocabularyRelation> relations;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "admin_vocabulary_categories",
            joinColumns = @JoinColumn(name = "vocabulary_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @BatchSize(size = 50)
    private Set<Category> categories;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "admin_vocabulary_tags",
            joinColumns = @JoinColumn(name = "vocabulary_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @BatchSize(size = 50)
    private Set<Tag> tags;
}
