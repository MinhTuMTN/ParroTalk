package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftSegmentResponse {
    private UUID segmentId;
    private String userAnswer;
    private boolean isCorrect;
    private int score;
    private int replayCount;
    private int hintCount;
    private LocalDateTime updatedAt;
}
