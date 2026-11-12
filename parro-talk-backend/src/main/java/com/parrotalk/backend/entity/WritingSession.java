package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.WritingMode;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
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

@Entity
@Table(name = "writing_sessions", indexes = {
        @Index(name = "idx_writing_sessions_user_created", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE writing_sessions SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class WritingSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "input_text", nullable = false, columnDefinition = "TEXT")
    private String inputText;

    @Column(name = "input_language", nullable = false, length = 16)
    @Builder.Default
    private String inputLanguage = "VI";

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private WritingMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "scale_type", nullable = false, length = 16)
    private DifficultyScale scaleType;

    @Column(name = "target_level", nullable = false, length = 16)
    private String targetLevel;

    @Column(name = "core_idea_summary_vn", columnDefinition = "TEXT")
    private String coreIdeaSummaryVn;

    @Column(name = "combination_guide_vn", columnDefinition = "TEXT")
    private String combinationGuideVn;

    @Column(name = "user_draft", columnDefinition = "TEXT")
    private String userDraft;

    @Column(name = "response_json", nullable = false, columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "grammar_check_json", columnDefinition = "TEXT")
    private String grammarCheckJson;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<WritingGeneratedItem> generatedItems = new ArrayList<>();
}

