package com.parrotalk.backend.dto.practice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeStatisticsDto {
    private int todayLearned;
    private int totalMastered;
    private int currentStreak;
    private int reviewDue;
    private double retentionRate;
}
