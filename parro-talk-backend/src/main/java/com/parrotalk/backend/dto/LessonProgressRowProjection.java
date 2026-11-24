package com.parrotalk.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight projection for fetching lesson progress along with draft segments in a single query.
 */
public interface LessonProgressRowProjection {

    /**
     * Get the current segment id.
     *
     * @return Current segment id
     */
    UUID getCurrentSegmentId();

    /**
     * Get the last recorded lesson progress ratio.
     *
     * @return Last progress ratio
     */
    double getLastProgress();

    /**
     * Get the progress last updated timestamp.
     *
     * @return Progress updated timestamp
     */
    LocalDateTime getUpdatedAt();

    /**
     * Get the draft segment id, or null when no draft segments exist.
     *
     * @return Segment id or null
     */
    UUID getSegmentId();

    /**
     * Get the user's draft answer text.
     *
     * @return User answer or null
     */
    String getUserAnswer();

    /**
     * Get the draft segment score, or null when no draft segment exists.
     *
     * @return Draft score or null
     */
    Integer getScore();

    /**
     * Get the draft segment replay count, or null when no draft segment exists.
     *
     * @return Replay count or null
     */
    Integer getReplayCount();

    /**
     * Get the draft segment hint count, or null when no draft segment exists.
     *
     * @return Hint count or null
     */
    Integer getHintCount();

    /**
     * Get whether the draft segment answer is correct, or null when no draft segment exists.
     *
     * @return Correct flag or null
     */
    Boolean getCorrect();

    /**
     * Get the draft segment last updated timestamp, or null when no draft segment exists.
     *
     * @return Draft updated timestamp or null
     */
    LocalDateTime getDraftUpdatedAt();
}
