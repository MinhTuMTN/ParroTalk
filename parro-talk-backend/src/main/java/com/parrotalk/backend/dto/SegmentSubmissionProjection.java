package com.parrotalk.backend.dto;

import java.util.UUID;

/**
 * Lightweight projection for loading segment and lesson metadata in {@code submitAnswer}.
 */
public interface SegmentSubmissionProjection {

    /**
     * Get the segment id.
     *
     * @return Segment id
     */
    UUID getId();

    /**
     * Get the raw lesson id stored on the segment foreign key.
     *
     * @return Raw lesson id
     */
    UUID getRawLessonId();

    /**
     * Get the active (non-deleted) lesson id from the joined lesson row, or null if soft-deleted.
     *
     * @return Active lesson id or null
     */
    UUID getActiveLessonId();

    /**
     * Get the transcription segment text.
     *
     * @return Segment text
     */
    String getText();

    /**
     * Get the serialized blanks JSON payload for the segment.
     *
     * @return Serialized blanks JSON or null
     */
    String getBlanksJson();

    /**
     * Get the cached total segment count on the joined lesson.
     *
     * @return Lesson total segments count or null
     */
    Integer getLessonTotalSegments();
}
