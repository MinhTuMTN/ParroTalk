package com.parrotalk.backend.dto.moderation;

import java.time.LocalDateTime;
import java.util.UUID;

import com.parrotalk.backend.constant.ModerationField;
import com.parrotalk.backend.entity.ModerationEvent;

/**
 * One entry of the audit trail of a moderated item.
 *
 * @param id        Event id
 * @param field     Field that changed
 * @param oldValue  Previous value as text
 * @param newValue  New value as text
 * @param note      Comment written by the actor
 * @param actor     Who performed the change
 * @param createdAt When the change happened
 */
public record ModerationEventResponse(
        UUID id,
        ModerationField field,
        String oldValue,
        String newValue,
        String note,
        UserSummaryResponse actor,
        LocalDateTime createdAt) {

    /**
     * Map an audit entity.
     *
     * @param event Moderation event
     * @return Response projection
     */
    public static ModerationEventResponse from(ModerationEvent event) {
        return new ModerationEventResponse(
                event.getId(),
                event.getField(),
                event.getOldValue(),
                event.getNewValue(),
                event.getNote(),
                UserSummaryResponse.from(event.getActor()),
                event.getCreatedAt());
    }
}
