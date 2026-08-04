package com.parrotalk.backend.dto.moderation;

import java.util.UUID;

import com.parrotalk.backend.constant.ModerationPriority;
import com.parrotalk.backend.constant.ModerationStatus;

import jakarta.validation.constraints.Size;

/**
 * Partial update of the triage state of a moderated item.
 *
 * <p>
 * Every field is optional; {@code null} means "leave unchanged". Clearing the
 * assignee needs an explicit {@code unassign} flag because {@code null} alone
 * cannot express the difference between "no change" and "remove".
 * </p>
 *
 * @param status         New status
 * @param priority       New priority
 * @param assigneeId     Admin to assign the item to
 * @param unassign       When {@code true}, clear the current assignee
 * @param resolutionNote Closing note
 * @param note           Free-text comment stored on the audit entries
 */
public record ModerationUpdateRequest(
        ModerationStatus status,
        ModerationPriority priority,
        UUID assigneeId,
        boolean unassign,
        @Size(max = 1000, message = "resolutionNote must not exceed 1000 characters") String resolutionNote,
        @Size(max = 1000, message = "note must not exceed 1000 characters") String note) {
}
