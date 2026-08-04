package com.parrotalk.backend.dto.practice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeResultDto {
    private UUID sessionId;
    private int totalQuestions;
    private int correctAnswers;
    private double accuracy;
    private int xpEarned;
    private int newMasteredWords;
    private int streak;
}
