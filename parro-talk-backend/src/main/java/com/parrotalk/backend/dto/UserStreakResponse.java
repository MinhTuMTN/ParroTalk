package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStreakResponse {

    private int currentStreak;
    private int longestStreak;
    private LocalDate longestStreakAchievedAt;
    private boolean hasStudiedToday;
    private int weeklyGoalDays;
    private int weeklyCompletedDays;
    private List<WeeklyProgressDay> weeklyProgress;
    private int calendarYear;
    private List<Integer> calendarYears;
    private List<CalendarDay> calendar;
    private Statistics statistics;
    private List<Achievement> achievements;
    private Motivation motivation;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyProgressDay {
        private String day;
        private LocalDate date;
        private boolean studied;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarDay {
        private LocalDate date;
        private boolean studied;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Statistics {
        private int totalStudyDays;
        private int totalStudyMinutes;
        private int lessonsCompleted;
        private int averageDailyStudyMinutes;
        private int currentStreak;
        private int longestStreak;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Achievement {
        private String code;
        private String title;
        private boolean achieved;
        private LocalDate achievedAt;
        private Integer progress;
        private Integer target;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Motivation {
        private String title;
        private String quote;
        private String author;
        private String warningMessage;
    }
}
