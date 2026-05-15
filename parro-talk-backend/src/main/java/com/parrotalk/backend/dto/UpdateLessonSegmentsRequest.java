package com.parrotalk.backend.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * Request to update segments of a lesson.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
public class UpdateLessonSegmentsRequest {

    /** List of segments to update or create */
    @NotNull(message = "segments is required")
    @Valid
    private List<UpsertSegmentRequest> segments;

    /** List of segments to delete */
    private List<UUID> deletedSegmentIds;
}
