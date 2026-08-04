package com.parrotalk.backend.constant;

/**
 * Domain object an audit event belongs to.
 *
 * <p>
 * Used as discriminator of the shared {@code moderation_events} table so the
 * two independent domains can reuse one append-only audit trail without
 * knowing about each other.
 * </p>
 */
public enum ModerationTargetType {

    /** Event on a {@code lesson_reports} row. */
    LESSON_REPORT,

    /** Event on an {@code app_feedbacks} row. */
    APP_FEEDBACK
}
