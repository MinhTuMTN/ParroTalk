package com.parrotalk.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.LessonVisibilityStatus;

/**
 * Lesson response DTO.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {
    /** Lesson id */
    private UUID id;
    /** Lesson status */
    private LessonStatus status;
    /** Lesson visibility status */
    private LessonVisibilityStatus visibilityStatus;
    /** Lesson progress */
    private int progress;
    /** Audio file url */
    private String fileUrl;
    /** Audio file type */
    private String mediaType;
    /** Audio source type */
    private String sourceType;
    /** Audio created date */
    private LocalDateTime createdAt;
    /** Audio title */
    private String title;
    /** Audio description */
    private String description;
    /** Audio duration */
    private Integer duration;
    /** Categories */
    private Set<CategoryResponse> categories;
    /** Segments */
    private List<TranscriptionResponse> segments;
}
