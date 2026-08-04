package com.parrotalk.backend.dto.practice;

import com.parrotalk.backend.constant.PracticeSessionStatus;
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
public class PracticeSessionDto {
    private UUID sessionId;
    private PracticeSessionStatus status;
    private LocalDateTime startedAt;
    private List<PracticeQuestionDto> questions;
}
