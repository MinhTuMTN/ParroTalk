package com.parrotalk.backend.dto;

/**
 * Projection for batch lesson retrieval across multiple library sections.
 */
public interface SectionLessonProjection {

    String getSectionKey();

    String getId();

    String getTitle();

    Double getProgress();

    String getThumbnailUrl();

    String getFileUrl();

    Integer getDuration();
}

