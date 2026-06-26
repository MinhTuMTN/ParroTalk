package com.parrotalk.backend.service;

import com.parrotalk.backend.config.AppSetting;
import com.parrotalk.backend.entity.UserActiveDay;
import com.parrotalk.backend.entity.UserProgress;
import com.parrotalk.backend.entity.UserStreak;
import com.parrotalk.backend.entity.StudyActivity;
import com.parrotalk.backend.repository.StudyActivityRepository;
import com.parrotalk.backend.repository.UserActiveDayRepository;
import com.parrotalk.backend.repository.UserProgressRepository;
import com.parrotalk.backend.repository.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLearningActivityService {

    private final UserProgressRepository progressRepository;
    private final UserStreakRepository streakRepository;
    private final UserActiveDayRepository activeDayRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final AppSetting appSetting;

    @Transactional
    public void recordCompletedLesson(UUID userId, double score) {
        updateProgress(userId, score);
        updateStudyActivity(userId, 0, 0, 1);
        recordStudyDay(userId);
    }

    @Transactional
    public void recordStudyDay(UUID userId) {
        LocalDate today = today();
        updateActiveDay(userId, today);
        updateStreak(userId, today);
    }

    @Transactional
    public void recordSegmentAttempt(UUID userId, boolean completedSegment, int studySecondsDelta) {
        StudyActivity activity = updateStudyActivity(
                userId,
                Math.max(studySecondsDelta, 0),
                completedSegment ? 1 : 0,
                0);
        if (activity.getSegmentsCompleted() >= 2) {
            recordStudyDay(userId);
        }
    }

    public LocalDate today() {
        return LocalDate.now(ZoneId.of(appSetting.getDefaultTimeZone()));
    }

    private void updateProgress(UUID userId, double newScore) {
        UserProgress progress = progressRepository.findById(userId)
                .orElse(UserProgress.builder()
                        .userId(userId)
                        .build());

        int count = progress.getTotalLessonsCompleted() == null ? 0 : progress.getTotalLessonsCompleted();
        double total = progress.getTotalScore() == null ? 0.0 : progress.getTotalScore();
        progress.setTotalLessonsCompleted(count + 1);
        progress.setTotalScore(total + newScore);
        progress.setAvgScore((total + newScore) / (count + 1));
        progress.setLastActivityDate(LocalDateTime.now(ZoneId.of(appSetting.getDefaultTimeZone())));
        progressRepository.save(progress);
    }

    private void updateActiveDay(UUID userId, LocalDate today) {
        if (activeDayRepository.findByUserIdAndActiveDate(userId, today).isEmpty()) {
            activeDayRepository.save(UserActiveDay.builder()
                    .userId(userId)
                    .activeDate(today)
                    .build());
        }
    }

    private void updateStreak(UUID userId, LocalDate today) {
        UserStreak streak = streakRepository.findById(userId)
                .orElse(UserStreak.builder()
                        .userId(userId)
                        .build());

        LocalDate lastActive = streak.getLastActiveDate();
        if (lastActive == null) {
            streak.setCurrentStreak(1);
            streak.setLongestStreak(1);
            streak.setLastActiveDate(today);
        } else if (!lastActive.equals(today)) {
            if (lastActive.plusDays(1).equals(today)) {
                int nextCurrentStreak = (streak.getCurrentStreak() == null ? 0 : streak.getCurrentStreak()) + 1;
                streak.setCurrentStreak(nextCurrentStreak);
                streak.setLongestStreak(Math.max(
                        streak.getLongestStreak() == null ? 0 : streak.getLongestStreak(),
                        nextCurrentStreak));
            } else {
                streak.setCurrentStreak(1);
                streak.setLongestStreak(Math.max(
                        streak.getLongestStreak() == null ? 0 : streak.getLongestStreak(),
                        1));
            }
            streak.setLastActiveDate(today);
        }

        streakRepository.save(streak);
    }

    private StudyActivity updateStudyActivity(
            UUID userId,
            int studySecondsDelta,
            int completedSegmentsDelta,
            int completedLessonsDelta) {
        LocalDate today = today();
        LocalDateTime now = LocalDateTime.now(ZoneId.of(appSetting.getDefaultTimeZone()));
        StudyActivity activity = studyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .orElse(StudyActivity.builder()
                        .userId(userId)
                        .activityDate(today)
                        .firstActivityAt(now)
                        .lastActivityAt(now)
                        .build());

        activity.setStudySeconds(activity.getStudySeconds() + studySecondsDelta);
        activity.setSegmentsCompleted(activity.getSegmentsCompleted() + completedSegmentsDelta);
        activity.setLessonsCompleted(activity.getLessonsCompleted() + completedLessonsDelta);
        activity.setLastActivityAt(now);
        return studyActivityRepository.save(activity);
    }
}
