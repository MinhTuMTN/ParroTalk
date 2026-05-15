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

    public LessonWithProgressDTO(UUID id, String title, double progress) {
        this.id = id;
        this.title = title;
        this.progress = (int) Math.round(progress);
    }
}
