package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.VocabularyListStatus;
import com.parrotalk.backend.constant.VocabularyListVisibility;
import jakarta.persistence.CascadeType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "vocabulary_lists", indexes = {
        @Index(name = "idx_vocabulary_lists_owner", columnList = "owner_id"),
        @Index(name = "idx_vocabulary_lists_share_token", columnList = "share_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE vocabulary_lists SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class VocabularyList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_cefr", length = 10)
    private String targetCefr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VocabularyListVisibility visibility = VocabularyListVisibility.PRIVATE;

    @Column(name = "share_token", unique = true, length = 64)
    private String shareToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private VocabularyListStatus status = VocabularyListStatus.DRAFT;

    @Column(name = "cloned_from_list_id")
    private UUID clonedFromListId;

    @OneToMany(mappedBy = "vocabularyList", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VocabularyListItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "vocabularyList", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VocabularyListShare> shares = new ArrayList<>();
}

