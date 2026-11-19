package com.parrotalk.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.parrotalk.backend.dto.dictation.BlankResultItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftSegmentResponse {
    private UUID segmentId;
    private String userAnswer;
    private int score;
    private int replayCount;
    private int hintCount;
    private boolean correct;
    private LocalDateTime updatedAt;
    private List<BlankResultItem> blankResults;
    private Double accuracy;
}
