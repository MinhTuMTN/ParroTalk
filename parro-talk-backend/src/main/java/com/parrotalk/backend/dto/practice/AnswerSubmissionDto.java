package com.parrotalk.backend.dto.practice;

import com.parrotalk.backend.constant.Sm2Rating;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmissionDto {
    @NotNull
    private UUID sessionId;
    
    @NotNull
    private UUID userVocabularyId;
    
    private String answer; // E.g., typed word or selected option
    
    private Sm2Rating rating; // Used for FLASHCARD mode
    
    private Long timeSpentMs;
}
