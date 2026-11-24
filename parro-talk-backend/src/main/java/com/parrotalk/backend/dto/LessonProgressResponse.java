package com.parrotalk.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.parrotalk.backend.constant.DictationMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressResponse {
    private UUID userId;
    private UUID lessonId;
    private DictationMode mode;
    private UUID currentSegmentId;
    private int lastPositionSeconds;
    private double lastProgress;
    private LocalDateTime updatedAt;
    private List<DraftSegmentResponse> draftSegments;
}
