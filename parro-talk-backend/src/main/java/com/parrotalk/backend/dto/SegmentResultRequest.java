package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SegmentResultRequest {
    private Long segmentId;
    private int hintWords;
    private int replayCount;
    private int attempts;
    private String userAnswer;
}
