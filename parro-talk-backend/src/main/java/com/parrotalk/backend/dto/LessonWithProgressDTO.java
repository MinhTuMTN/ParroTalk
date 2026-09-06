package com.parrotalk.backend.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lesson projection with current user progress.
 */
@Getter
@Setter
@NoArgsConstructor
public class LessonWithProgressDTO {

    public static final String DEFAULT_THUMBNAIL_URL = "https://res.cloudinary.com/dgrjyts1r/image/upload/v1778946072/default-audio-thumbnail.png";

    private UUID id;
    private String title;
    private int progress;
    private String thumbnailUrl;
    private String url;
    private Integer duration;

    public LessonWithProgressDTO(UUID id, String title, double progress, String thumbnailUrl, String url,
            Integer duration) {
        this.id = id;
        this.title = title;
        this.progress = (int) Math.round(progress);
        this.thumbnailUrl = (thumbnailUrl != null && !thumbnailUrl.isBlank()) ? thumbnailUrl : DEFAULT_THUMBNAIL_URL;
        this.url = url;
        this.duration = duration;
    }

    public void setProgress(double progress) {
        this.progress = (int) Math.round(progress);
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = (thumbnailUrl != null && !thumbnailUrl.isBlank()) ? thumbnailUrl : DEFAULT_THUMBNAIL_URL;
    }
}
