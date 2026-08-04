package com.parrotalk.backend.constant;

/**
 * Field of a moderated item that an audit event describes.
 */
public enum ModerationField {

    /** The item was created. */
    CREATED,

    /** {@link ModerationStatus} changed. */
    STATUS,

    /** {@link ModerationPriority} changed. */
    PRIORITY,

    /** Assignee was set, changed or cleared. */
    ASSIGNEE,

    /** Resolution note was written or updated. */
    RESOLUTION_NOTE
}
