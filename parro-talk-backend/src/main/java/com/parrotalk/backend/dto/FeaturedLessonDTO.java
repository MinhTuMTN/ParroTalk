package com.parrotalk.backend.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Featured lesson DTO.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedLessonDTO {

    /** Lesson ID */
    private UUID id;

    /** Lesson title */
    private String title;

    /** Lesson duration (seconds) */
    private Integer duration;

    /** Thumbnail URL */
    private String thumbnailUrl;

    /** File URL */
    private String fileUrl;

    /** Categories */
    private List<String> categories;

    /** Media type */
    private String mediaType;
}
