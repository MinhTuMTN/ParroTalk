package com.parrotalk.backend.constant;

/**
 * Lifecycle of a moderated item (lesson report or app feedback).
 *
 * <p>
 * Shared by both domains on purpose: the triage workflow is identical, only
 * the payload differs.
 * </p>
 */
public enum ModerationStatus {

    /** Just submitted, nobody looked at it yet. */
    OPEN,

    /** Someone is actively working on it. */
    IN_REVIEW,

    /** Handled, the underlying problem was fixed or accepted. */
    RESOLVED,

    /** Reviewed and declined (not reproducible, invalid, out of scope). */
    REJECTED,

    /** Same problem as an already tracked item. */
    DUPLICATE;

    /**
     * Whether the item is closed and no longer part of the working queue.
     *
     * @return {@code true} for a terminal status
     */
    public boolean isTerminal() {
        return this == RESOLVED || this == REJECTED || this == DUPLICATE;
    }
}
