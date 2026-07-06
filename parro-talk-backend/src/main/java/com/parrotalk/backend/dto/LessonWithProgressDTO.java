package com.parrotalk.backend.dto;

import java.util.UUID;

import lombok.Getter;

/**
 * Lesson projection with current user progress.
 */
@Getter
public class LessonWithProgressDTO {

    private final UUID id;
    private final String title;
    private final int progress;
    private final String thumbnail;
    private final String url;
    private final Integer duration;

    public LessonWithProgressDTO(UUID id, String title, double progress, String thumbnail, String url,
            Integer duration) {
        this.id = id;
        this.title = title;
        this.progress = (int) Math.round(progress);
        this.thumbnail = thumbnail;
        this.url = url;
        this.duration = duration;
    }
}
