package com.parrotalk.backend.dto;

import com.parrotalk.backend.constant.LessonVisibilityStatus;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import lombok.Getter;
import lombok.Setter;

/**
 * Update lesson general information request.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
public class UpdateLessonInfoRequest {

    /** Title */
    @NotBlank(message = "Title is required")
    private String title;

    /** Source */
    @NotBlank(message = "Source is required")
    @URL(message = "Source must be a valid URL")
    private String source;

    /** Status */
    private LessonVisibilityStatus status;

    /** Category IDs */
    private List<String> categoryIds;
}
