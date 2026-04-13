package com.parrotalk.backend.constant;

/**
 * Lesson status.
 * 
 * @author MinhTuMTN
 */
public enum LessonStatus {
    /** Pending (Lesson uploaded and waiting for processing) */
    PENDING,
    /** Processing (Lesson is being transcribed) */
    PROCESSING,
    /** Done (Lesson has been transcribed and ready for learning) */
    DONE,
    /** Failed (Lesson failed to process) */
    FAILED
}
