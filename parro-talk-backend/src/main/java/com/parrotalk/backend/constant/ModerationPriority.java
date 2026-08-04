package com.parrotalk.backend.constant;

/**
 * Triage priority of a moderated item.
 */
public enum ModerationPriority {

    /** Cosmetic, can wait. */
    LOW,

    /** Default priority for new items. */
    MEDIUM,

    /** Blocks part of the learning flow. */
    HIGH,

    /** Blocks the product, needs immediate attention. */
    URGENT
}
