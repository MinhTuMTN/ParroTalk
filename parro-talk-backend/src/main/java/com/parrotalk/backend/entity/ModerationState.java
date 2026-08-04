package com.parrotalk.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.parrotalk.backend.constant.ModerationPriority;
import com.parrotalk.backend.constant.ModerationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Triage state shared by every moderated item.
 *
 * <p>
 * Embedded into {@link LessonReport} and {@link AppFeedback} instead of being
 * duplicated in both entities. The two domains stay independent (separate
 * tables, separate services) while the workflow columns and the code that
 * mutates them exist exactly once.
 * </p>
 *
 * @author MinhTuMTN
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ModerationState {

    /** Current lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ModerationStatus status = ModerationStatus.OPEN;

    /** Triage priority. */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private ModerationPriority priority = ModerationPriority.MEDIUM;

    /** Admin currently responsible for the item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /** Explanation written when the item was closed. */
    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    /** Time the item entered a terminal status. */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Build the state a freshly submitted item starts with.
     *
     * @return New {@link ModerationState} in {@link ModerationStatus#OPEN}
     */
    public static ModerationState initial() {
        return new ModerationState();
    }

    /**
     * Get the assignee id without initializing the lazy proxy.
     *
     * @return Assignee id, or {@code null} when unassigned
     */
    public UUID getAssigneeId() {
        return assignee == null ? null : assignee.getId();
    }
}
