package com.parrotalk.backend.constant;

/**
 * Section type.
 * 
 * @author MinhTuMTN
 */
public enum SectionType {
    /** The lesson is being uploaded. */
    IN_PROGRESS,
    /** The lesson which user is currently learing */
    USER_PROCESSING,
    /** Lesson uploaded by user */
    USER_UPLOADED,
    /** Lesson by category */
    CATEGORY,
    /** Lesson by program */
    PROGRAM
}
