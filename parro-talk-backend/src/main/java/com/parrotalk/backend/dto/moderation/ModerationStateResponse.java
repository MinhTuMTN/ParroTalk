package com.parrotalk.backend.dto.moderation;

import java.time.LocalDateTime;
import java.util.UUID;

import com.parrotalk.backend.constant.ModerationPriority;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.entity.ModerationState;

/**
 * Triage state exposed to clients, shared by both moderated domains.
 *
 * @param status         Current lifecycle status
 * @param priority       Triage priority
 * @param assigneeId     Assigned admin id, {@code null} when unassigned
 * @param resolutionNote Closing note
 * @param resolvedAt     Time the item was closed
 */
public record ModerationStateResponse(
        ModerationStatus status,
        ModerationPriority priority,
        UUID assigneeId,
        String resolutionNote,
        LocalDateTime resolvedAt) {

    /**
     * Map the embeddable triage state.
     *
     * @param state Moderation state
     * @return Response projection
     */
    public static ModerationStateResponse from(ModerationState state) {
        return new ModerationStateResponse(
                state.getStatus(),
                state.getPriority(),
                state.getAssigneeId(),
                state.getResolutionNote(),
                state.getResolvedAt());
    }
}
