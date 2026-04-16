package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressResponse {
    private UUID userId;
    private UUID lessonId;
    private UUID currentSegmentId;
    private int lastPositionSeconds;
    private double lastProgress;
    private LocalDateTime updatedAt;
    private List<DraftSegmentResponse> draftSegments;
}
