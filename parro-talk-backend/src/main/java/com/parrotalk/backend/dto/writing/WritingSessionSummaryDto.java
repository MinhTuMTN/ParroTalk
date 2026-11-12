package com.parrotalk.backend.dto.writing;

import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.WritingMode;
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
public class WritingSessionSummaryDto {
    private UUID id;
    private String inputText;
    private String inputLanguage;
    private WritingMode mode;
    private DifficultyScale scaleType;
    private String targetLevel;
    private String coreIdeaSummaryVn;
    private boolean hasUserDraft;
    private LocalDateTime createdAt;
}

