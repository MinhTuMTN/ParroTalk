package com.parrotalk.backend.dto;

import java.util.List;

import com.parrotalk.backend.entity.TranscriptionSegment;

/**
 * Result of building transcription segment responses along with any segments missing precomputed blanks.
 *
 * @param segments        Built segment response DTOs
 * @param missingSegments Segments that required on-the-fly blank generation
 * @param translatedCount Number of segments that have a translation
 */
public record SegmentBuildResult(
        List<TranscriptionResponse> segments,
        List<TranscriptionSegment> missingSegments,
        long translatedCount) {
}
