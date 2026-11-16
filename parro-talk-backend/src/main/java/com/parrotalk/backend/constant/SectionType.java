package com.parrotalk.backend.constant;

/**
 * Section type.
 * 
 * @author MinhTuMTN
 */
public enum SectionType {
    /** The lesson which user is currently learning */
    IN_PROGRESS,
    /** Legacy section type kept for backwards-compatibility with existing DB rows */
    @Deprecated
    USER_PROCESSING,
    /** Legacy section type kept for backwards-compatibility with existing DB rows */
    @Deprecated
    USER_UPLOADED,
    /** Lesson by category */
    CATEGORY,
    /** Lesson by program */
    PROGRAM
}
