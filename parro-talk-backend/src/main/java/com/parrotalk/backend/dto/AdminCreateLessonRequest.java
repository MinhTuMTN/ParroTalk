package com.parrotalk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import lombok.Getter;
import lombok.Setter;

/**
 * Admin create lesson request
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
public class AdminCreateLessonRequest {

    /** Title */
    @NotBlank(message = "Title is required")
    private String title;

    /** Source URL */
    @NotBlank(message = "Source is required")
    @URL(message = "Source must be a valid URL")
    private String source;
}
