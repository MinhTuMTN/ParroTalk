package com.parrotalk.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to update or create a segment.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
public class UpsertSegmentRequest {

    /** Segment ID (null if create new segment) */
    private UUID id;

    /** Segment anwser */
    @NotBlank(message = "Segment text is required")
    private String text;

    /** Segment start time */
    @NotNull(message = "startTime is required")
    @Min(value = 0, message = "startTime must be >= 0")
    private Double startTime;

    /** Segment end time */
    @NotNull(message = "endTime is required")
    @Min(value = 0, message = "endTime must be >= 0")
    private Double endTime;

    /** Segment order */
    @NotNull(message = "order is required")
    @Min(value = 0, message = "order must be >= 0")
    private Integer order;
}
