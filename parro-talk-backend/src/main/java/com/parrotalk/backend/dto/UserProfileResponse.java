package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private UUID id;
    private String fullName;
    private String email;
    private int totalLessonsCompleted;
    private double totalScore;
    private double avgScore;
    private int currentStreak;
    private int longestStreak;
    private List<LocalDate> activeDays;
}
