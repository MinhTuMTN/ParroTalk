package com.parrotalk.backend.entity;

import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

import com.parrotalk.backend.constant.ModerationField;
import com.parrotalk.backend.constant.ModerationTargetType;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Append-only audit entry describing one change on a moderated item.
 *
 * <p>
 * Intentionally polymorphic ({@code target_type} + {@code target_id}) rather
 * than two identical per-domain tables. The trade-off is the loss of a
 * database foreign key on {@code target_id}; that is acceptable because the
 * table is an immutable log that must outlive soft-deleted targets, and it
 * keeps the two domains from depending on each other.
 * </p>
 *
 * @author MinhTuMTN
 */
@Entity
@Table(name = "moderation_events", indexes = {
        @Index(name = "idx_moderation_events_target", columnList = "target_type, target_id, created_at"),
        @Index(name = "idx_moderation_events_actor", columnList = "actor_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("is_deleted = false")
public class ModerationEvent extends BaseEntity {

    /** Moderation event ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Domain the target belongs to. */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ModerationTargetType targetType;

    /** Identifier of the moderated item. */
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    /** User who performed the change. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    /** Field that changed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "field", nullable = false, length = 30)
    private ModerationField field;

    /** Previous value rendered as text, {@code null} when there was none. */
    @Column(name = "old_value", length = 255)
    private String oldValue;

    /** New value rendered as text, {@code null} when the value was cleared. */
    @Column(name = "new_value", length = 255)
    private String newValue;

    /** Optional comment written by the actor. */
    @Column(name = "note", length = 1000)
    private String note;
}
