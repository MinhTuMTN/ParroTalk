package com.parrotalk.backend.constant;

/**
 * Category of an application-level feedback.
 *
 * <p>
 * Unrelated to lesson data - this classifies feedback about the product
 * itself.
 * </p>
 */
public enum FeedbackCategory {

    /** Sign-in, sign-up or OAuth problems. */
    LOGIN,

    /** Functional defect. */
    BUG,

    /** Slowness or resource usage. */
    PERFORMANCE,

    /** Request for a new capability. */
    FEATURE_REQUEST,

    /** Billing or checkout problem. */
    PAYMENT,

    /** Interface or usability problem. */
    UI_UX,

    /** Application crash. */
    CRASH,

    /** Data synchronization problem. */
    SYNC,

    /** Anything that does not fit the categories above. */
    OTHER
}
