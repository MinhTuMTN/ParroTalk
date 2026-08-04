package com.parrotalk.backend.entity;

import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.parrotalk.backend.constant.FeedbackCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Feedback or bug report about the application itself.
 *
 * <p>
 * Deliberately independent from {@link LessonReport}: no foreign key to
 * lesson or segment, its own table and its own service. The only thing shared
 * is the generic {@link ModerationState} triage workflow.
 * </p>
 *
 * @author MinhTuMTN
 */
@Entity
@Table(name = "app_feedbacks", indexes = {
        @Index(name = "idx_app_feedbacks_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_app_feedbacks_category_status", columnList = "category, status"),
        @Index(name = "idx_app_feedbacks_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_app_feedbacks_assignee_status", columnList = "assignee_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE app_feedbacks SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class AppFeedback extends BaseEntity {

    /** App feedback ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** User who submitted the feedback. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Feedback category. */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private FeedbackCategory category;

    /** Short summary. */
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    /** Detailed description. */
    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    /** Shared triage state. */
    @Embedded
    @Builder.Default
    private ModerationState moderationState = ModerationState.initial();
}
