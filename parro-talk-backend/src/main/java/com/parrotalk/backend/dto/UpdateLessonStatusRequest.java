package com.parrotalk.backend.dto;

import com.parrotalk.backend.constant.LessonVisibilityStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Update lesson status request.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
public class UpdateLessonStatusRequest {

    /** Lesson Visibility Status */
    @NotNull(message = "Status is required")
    private LessonVisibilityStatus status;
}
