package com.parrotalk.backend.constant;

/**
 * Kind of lesson data a report targets.
 *
 * <p>
 * Acts as the discriminator of the single {@code lesson_reports} table.
 * {@link #TRANSLATION} reports always reference a segment, {@link #FILE}
 * reports never do.
 * </p>
 */
public enum LessonReportType {

    /** Problem with the audio/video file of the lesson. */
    FILE,

    /** Problem with the translation of a single segment. */
    TRANSLATION
}
