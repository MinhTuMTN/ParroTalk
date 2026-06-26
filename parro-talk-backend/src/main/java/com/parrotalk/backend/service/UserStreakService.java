package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.UserStreakResponse;
import com.parrotalk.backend.entity.UserActiveDay;
import com.parrotalk.backend.entity.UserProgress;
import com.parrotalk.backend.entity.StudyActivity;
import com.parrotalk.backend.repository.StudyActivityRepository;
import com.parrotalk.backend.repository.UserActiveDayRepository;
import com.parrotalk.backend.repository.UserLessonHistoryRepository;
import com.parrotalk.backend.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserStreakService {

    private static final int WEEKLY_GOAL_DAYS = 7;
    private static final int CONSISTENCY_MASTER_TARGET = 365;
    private static final int TIME_BASED_ACHIEVEMENT_TARGET = 10;
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    private final UserActiveDayRepository activeDayRepository;
    private final UserLessonHistoryRepository historyRepository;
    private final UserProgressRepository progressRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final UserLearningActivityService learningActivityService;

    @Transactional(readOnly = true)
    public UserStreakResponse getUserStreak(UUID userId, Integer requestedCalendarYear) {
        LocalDate today = learningActivityService.today();
        List<LocalDate> activeDates = activeDayRepository.findAllByUserIdOrderByActiveDateAsc(userId)
                .stream()
                .map(UserActiveDay::getActiveDate)
                .distinct()
                .toList();
        Set<LocalDate> activeDateSet = new HashSet<>(activeDates);

        StreakSummary streakSummary = summarizeStreaks(activeDates, activeDateSet, today);
        List<UserStreakResponse.WeeklyProgressDay> weeklyProgress = buildWeeklyProgress(activeDateSet, today);
        int weeklyCompletedDays = (int) weeklyProgress.stream()
                .filter(UserStreakResponse.WeeklyProgressDay::isStudied)
                .count();

        int lessonsCompleted = resolveLessonsCompleted(userId);
        int totalStudyDays = activeDates.size();
        List<StudyActivity> studyActivities = studyActivityRepository.findAllByUserIdOrderByActivityDateAsc(userId);
        int totalStudyMinutes = studyActivities.stream()
                .mapToInt(activity -> activity.getStudySeconds() == null ? 0 : activity.getStudySeconds())
                .sum() / 60;
        int averageDailyStudyMinutes = totalStudyDays == 0 ? 0 : totalStudyMinutes / totalStudyDays;
        List<Integer> calendarYears = buildCalendarYears(activeDates, today.getYear());
        int calendarYear = resolveCalendarYear(requestedCalendarYear, calendarYears, today.getYear());

        return UserStreakResponse.builder()
                .currentStreak(streakSummary.currentStreak())
                .longestStreak(streakSummary.longestStreak())
                .longestStreakAchievedAt(streakSummary.longestStreakAchievedAt())
                .hasStudiedToday(activeDateSet.contains(today))
                .weeklyGoalDays(WEEKLY_GOAL_DAYS)
                .weeklyCompletedDays(weeklyCompletedDays)
                .weeklyProgress(weeklyProgress)
                .calendarYear(calendarYear)
                .calendarYears(calendarYears)
                .calendar(buildCalendar(activeDateSet, calendarYear))
                .statistics(UserStreakResponse.Statistics.builder()
                        .totalStudyDays(totalStudyDays)
                        .totalStudyMinutes(totalStudyMinutes)
                        .lessonsCompleted(lessonsCompleted)
                        .averageDailyStudyMinutes(averageDailyStudyMinutes)
                        .currentStreak(streakSummary.currentStreak())
                        .longestStreak(streakSummary.longestStreak())
                        .build())
                .achievements(buildAchievements(
                        activeDates,
                        studyActivities,
                        streakSummary.longestStreak(),
                        totalStudyDays))
                .motivation(buildMotivation(activeDateSet.contains(today), streakSummary.currentStreak()))
                .build();
    }

    private int resolveLessonsCompleted(UUID userId) {
        int progressCount = progressRepository.findById(userId)
                .map(UserProgress::getTotalLessonsCompleted)
                .orElse(0);
        long historyCount = historyRepository.countByUserId(userId);
        return Math.max(progressCount, Math.toIntExact(historyCount));
    }

    private StreakSummary summarizeStreaks(
            List<LocalDate> activeDates,
            Set<LocalDate> activeDateSet,
            LocalDate today) {
        if (activeDates.isEmpty()) {
            return new StreakSummary(0, 0, null);
        }

        LocalDate latestActiveDate = activeDates.get(activeDates.size() - 1);
        LocalDate currentAnchor = latestActiveDate.equals(today) || latestActiveDate.equals(today.minusDays(1))
                ? latestActiveDate
                : null;

        int currentStreak = 0;
        if (currentAnchor != null) {
            LocalDate cursor = currentAnchor;
            while (activeDateSet.contains(cursor)) {
                currentStreak++;
                cursor = cursor.minusDays(1);
            }
        }

        int longestStreak = 0;
        int runningStreak = 0;
        LocalDate previousDate = null;
        LocalDate longestStreakAchievedAt = null;
        for (LocalDate activeDate : activeDates) {
            runningStreak = previousDate != null && previousDate.plusDays(1).equals(activeDate)
                    ? runningStreak + 1
                    : 1;

            if (runningStreak >= longestStreak) {
                longestStreak = runningStreak;
                longestStreakAchievedAt = activeDate;
            }
            previousDate = activeDate;
        }

        return new StreakSummary(currentStreak, longestStreak, longestStreakAchievedAt);
    }

    private List<UserStreakResponse.WeeklyProgressDay> buildWeeklyProgress(
            Set<LocalDate> activeDateSet,
            LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<UserStreakResponse.WeeklyProgressDay> days = new ArrayList<>();
        for (int i = 0; i < WEEKLY_GOAL_DAYS; i++) {
            LocalDate date = weekStart.plusDays(i);
            days.add(UserStreakResponse.WeeklyProgressDay.builder()
                    .day(DAY_FORMATTER.format(date))
                    .date(date)
                    .studied(activeDateSet.contains(date))
                    .build());
        }
        return days;
    }

    private int resolveCalendarYear(Integer requestedCalendarYear, List<Integer> calendarYears, int currentYear) {
        if (requestedCalendarYear == null) {
            return currentYear;
        }

        return calendarYears.contains(requestedCalendarYear) ? requestedCalendarYear : currentYear;
    }

    private List<Integer> buildCalendarYears(List<LocalDate> activeDates, int currentYear) {
        int earliestYear = activeDates.isEmpty()
                ? currentYear
                : Math.min(activeDates.get(0).getYear(), currentYear);
        List<Integer> years = new ArrayList<>();
        for (int year = currentYear; year >= earliestYear; year--) {
            years.add(year);
        }
        return years;
    }

    private List<UserStreakResponse.CalendarDay> buildCalendar(
            Set<LocalDate> activeDateSet,
            int calendarYear) {
        LocalDate startDate = LocalDate.of(calendarYear, 1, 1);
        LocalDate endDate = LocalDate.of(calendarYear, 12, 31);
        List<UserStreakResponse.CalendarDay> calendar = new ArrayList<>();
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            calendar.add(UserStreakResponse.CalendarDay.builder()
                    .date(date)
                    .studied(activeDateSet.contains(date))
                    .build());
            date = date.plusDays(1);
        }
        return calendar;
    }

    private List<UserStreakResponse.Achievement> buildAchievements(
            List<LocalDate> activeDates,
            List<StudyActivity> studyActivities,
            int longestStreak,
            int totalStudyDays) {
        Map<Integer, LocalDate> streakAchievementDates = findStreakAchievementDates(activeDates, List.of(7, 30, 100));
        List<UserStreakResponse.Achievement> achievements = new ArrayList<>();
        achievements.add(buildStreakAchievement("STREAK_7_DAYS", "7-Day Streak", 7, longestStreak, streakAchievementDates));
        achievements.add(buildStreakAchievement("STREAK_30_DAYS", "30-Day Streak", 30, longestStreak, streakAchievementDates));
        achievements.add(buildStreakAchievement("STREAK_100_DAYS", "100-Day Streak", 100, longestStreak, streakAchievementDates));
        achievements.add(buildTimeBasedAchievement(
                "EARLY_BIRD",
                "Early Bird",
                studyActivities.stream()
                        .filter(activity -> activity.getFirstActivityAt().getHour() < 8)
                        .toList()));
        achievements.add(buildTimeBasedAchievement(
                "NIGHT_OWL",
                "Night Owl",
                studyActivities.stream()
                        .filter(activity -> activity.getLastActivityAt().getHour() >= 22)
                        .toList()));

        boolean consistencyAchieved = totalStudyDays >= CONSISTENCY_MASTER_TARGET;
        LocalDate consistencyAchievedAt = consistencyAchieved
                ? activeDates.get(CONSISTENCY_MASTER_TARGET - 1)
                : null;
        achievements.add(UserStreakResponse.Achievement.builder()
                .code("CONSISTENCY_MASTER")
                .title("Consistency Master")
                .achieved(consistencyAchieved)
                .achievedAt(consistencyAchievedAt)
                .progress(consistencyAchieved ? null : totalStudyDays)
                .target(consistencyAchieved ? null : CONSISTENCY_MASTER_TARGET)
                .build());
        return achievements;
    }

    private UserStreakResponse.Achievement buildTimeBasedAchievement(
            String code,
            String title,
            List<StudyActivity> qualifyingActivities) {
        boolean achieved = qualifyingActivities.size() >= TIME_BASED_ACHIEVEMENT_TARGET;
        return UserStreakResponse.Achievement.builder()
                .code(code)
                .title(title)
                .achieved(achieved)
                .achievedAt(achieved
                        ? qualifyingActivities.get(TIME_BASED_ACHIEVEMENT_TARGET - 1).getActivityDate()
                        : null)
                .progress(achieved ? null : qualifyingActivities.size())
                .target(achieved ? null : TIME_BASED_ACHIEVEMENT_TARGET)
                .build();
    }

    private UserStreakResponse.Achievement buildStreakAchievement(
            String code,
            String title,
            int target,
            int longestStreak,
            Map<Integer, LocalDate> streakAchievementDates) {
        boolean achieved = longestStreak >= target;
        return UserStreakResponse.Achievement.builder()
                .code(code)
                .title(title)
                .achieved(achieved)
                .achievedAt(achieved ? streakAchievementDates.get(target) : null)
                .progress(achieved ? null : longestStreak)
                .target(achieved ? null : target)
                .build();
    }

    private Map<Integer, LocalDate> findStreakAchievementDates(List<LocalDate> activeDates, List<Integer> targets) {
        Map<Integer, LocalDate> achievementDates = new HashMap<>();
        int runningStreak = 0;
        LocalDate previousDate = null;

        for (LocalDate activeDate : activeDates) {
            runningStreak = previousDate != null && previousDate.plusDays(1).equals(activeDate)
                    ? runningStreak + 1
                    : 1;

            for (int target : targets) {
                if (runningStreak >= target && !achievementDates.containsKey(target)) {
                    achievementDates.put(target, activeDate);
                }
            }
            previousDate = activeDate;
        }
        return achievementDates;
    }

    private UserStreakResponse.Motivation buildMotivation(boolean hasStudiedToday, int currentStreak) {
        String warningMessage = hasStudiedToday
                ? null
                : currentStreak > 0
                        ? "Study today to keep your " + currentStreak + "-day streak alive."
                        : "Start your first lesson today and begin your streak.";

        return UserStreakResponse.Motivation.builder()
                .title("Keep Going! \uD83D\uDCAA")
                .quote("Success is the sum of small efforts, repeated day in and day out.")
                .author("Robert Collier")
                .warningMessage(warningMessage)
                .build();
    }

    private record StreakSummary(int currentStreak, int longestStreak, LocalDate longestStreakAchievedAt) {
    }
}
