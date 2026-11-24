package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.CacheNames;
import com.parrotalk.backend.config.AppSetting;
import com.parrotalk.backend.entity.UserActiveDay;
import com.parrotalk.backend.entity.UserProgress;
import com.parrotalk.backend.entity.UserStreak;
import com.parrotalk.backend.entity.StudyActivity;
import com.parrotalk.backend.repository.StudyActivityRepository;
import com.parrotalk.backend.repository.UserActiveDayRepository;
import com.parrotalk.backend.repository.UserProgressRepository;
import com.parrotalk.backend.repository.UserStreakRepository;
import com.parrotalk.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
@RequiredArgsConstructor
public class UserLearningActivityService {

    private static final int DAILY_STREAK_SEGMENT_THRESHOLD = 2;
    private static final Duration STREAK_REDIS_TTL = Duration.ofHours(48);
    private static final String STREAK_UPDATED_KEY_PREFIX = "streak:updated:";
    private static final String STREAK_SEGMENTS_KEY_PREFIX = "streak:segments:";

    private final UserProgressRepository progressRepository;
    private final UserStreakRepository streakRepository;
    private final UserActiveDayRepository activeDayRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final UserRepository userRepository;
    private final AppSetting appSetting;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private DataSource dataSource;

    private volatile Boolean postgresUpsertsSupported;

    @Transactional
    @CacheEvict(value = CacheNames.ADMIN_USER_DETAIL_CACHE, key = "#userId")
    public void recordCompletedLesson(UUID userId, double score) {
        updateProgress(userId, score);
        updateStudyActivity(userId, 0, 0, 1);
        recordStudyDay(userId);
    }

    @Transactional
    @CacheEvict(value = CacheNames.ADMIN_USER_DETAIL_CACHE, key = "#userId")
    public void recordStudyDay(UUID userId) {
        LocalDate today = today();
        updateActiveDay(userId, today);
        updateStreak(userId, today);
        markStreakUpdatedInRedis(userId, today);
    }

    /**
     * Record a segment attempt and update study day and streak once the daily threshold is crossed.
     * Uses Redis to track whether today's streak was already updated and count daily completed segments.
     *
     * @param userId            User id
     * @param completedSegment  Whether the segment attempt was correct
     * @param studySecondsDelta Elapsed study seconds to add
     */
    @Transactional
    @CacheEvict(value = CacheNames.ADMIN_USER_DETAIL_CACHE, key = "#userId")
    public void recordSegmentAttempt(UUID userId, boolean completedSegment, int studySecondsDelta) {
        int completedDelta = completedSegment ? 1 : 0;
        int safeStudySecondsDelta = Math.max(studySecondsDelta, 0);

        if (redisTemplate == null) {
            int updatedSegmentsCompleted = updateStudyActivityAndReturnSegmentsCompleted(
                    userId,
                    safeStudySecondsDelta,
                    completedDelta,
                    0);
            int previousSegmentsCompleted = updatedSegmentsCompleted - completedDelta;
            if (completedSegment && previousSegmentsCompleted < DAILY_STREAK_SEGMENT_THRESHOLD
                    && updatedSegmentsCompleted >= DAILY_STREAK_SEGMENT_THRESHOLD) {
                recordStudyDay(userId);
            }
            return;
        }

        updateStudyActivity(userId, safeStudySecondsDelta, completedDelta, 0);
        if (!completedSegment) {
            return;
        }

        LocalDate today = today();
        String updatedKey = streakUpdatedKey(userId, today);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(updatedKey))) {
            return;
        }

        String segmentsKey = streakSegmentsKey(userId, today);
        Long completedToday = redisTemplate.opsForValue().increment(segmentsKey);
        if (completedToday != null && completedToday == 1L) {
            redisTemplate.expire(segmentsKey, STREAK_REDIS_TTL);
        }
        if (completedToday != null && completedToday >= DAILY_STREAK_SEGMENT_THRESHOLD) {
            recordStudyDay(userId);
        }
    }

    private void markStreakUpdatedInRedis(UUID userId, LocalDate today) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(streakUpdatedKey(userId, today), "1", STREAK_REDIS_TTL);
        }
    }

    private String streakUpdatedKey(UUID userId, LocalDate today) {
        return STREAK_UPDATED_KEY_PREFIX + userId + ":" + today;
    }

    private String streakSegmentsKey(UUID userId, LocalDate today) {
        return STREAK_SEGMENTS_KEY_PREFIX + userId + ":" + today;
    }

    public LocalDate today() {
        return LocalDate.now(ZoneId.of(appSetting.getDefaultTimeZone()));
    }

    private void updateProgress(UUID userId, double newScore) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of(appSetting.getDefaultTimeZone()));
        if (supportsPostgresUpserts()) {
            progressRepository.upsertLessonCompletion(userId, newScore, now);
        } else {
            UserProgress progress = progressRepository.findById(userId)
                    .orElse(UserProgress.builder()
                            .userId(userId)
                            .build());

            int count = progress.getTotalLessonsCompleted() == null ? 0 : progress.getTotalLessonsCompleted();
            double total = progress.getTotalScore() == null ? 0.0 : progress.getTotalScore();
            progress.setTotalLessonsCompleted(count + 1);
            progress.setTotalScore(total + newScore);
            progress.setAvgScore((total + newScore) / (count + 1));
            progress.setLastActivityDate(now);
            progressRepository.save(progress);
        }

        userRepository.updateLastActiveAt(userId, now);
    }

    /**
     * Record today as an active day for the user if not already recorded.
     *
     * @param userId User id
     * @param today  Current date in app timezone
     */
    private void updateActiveDay(UUID userId, LocalDate today) {
        if (supportsPostgresUpserts()) {
            activeDayRepository.insertIfAbsent(userId, today);
        } else if (!activeDayRepository.existsByUserIdAndActiveDate(userId, today)) {
            activeDayRepository.save(UserActiveDay.builder()
                    .userId(userId)
                    .activeDate(today)
                    .build());
        }
    }

    /**
     * Update the user's consecutive study streak when today has not yet been recorded.
     *
     * @param userId User id
     * @param today  Current date in app timezone
     */
    private void updateStreak(UUID userId, LocalDate today) {
        UserStreak streak = streakRepository.findById(userId)
                .orElseGet(() -> UserStreak.builder()
                        .userId(userId)
                        .build());

        LocalDate lastActive = streak.getLastActiveDate();
        if (lastActive == null) {
            streak.setCurrentStreak(1);
            streak.setLongestStreak(1);
            streak.setLastActiveDate(today);
            streakRepository.save(streak);
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
            streakRepository.save(streak);
        }
    }

    /**
     * Accumulate daily study activity counters for the given user and date.
     *
     * @param userId                 User id
     * @param studySecondsDelta      Study seconds delta to add
     * @param completedSegmentsDelta Completed segments delta to add
     * @param completedLessonsDelta  Completed lessons delta to add
     */
    private void updateStudyActivity(
            UUID userId,
            int studySecondsDelta,
            int completedSegmentsDelta,
            int completedLessonsDelta) {
        updateStudyActivityAndReturnSegmentsCompleted(
                userId,
                studySecondsDelta,
                completedSegmentsDelta,
                completedLessonsDelta);
    }

    /**
     * Accumulate daily study activity counters and return the updated segments_completed count.
     *
     * @param userId                 User id
     * @param studySecondsDelta      Study seconds delta to add
     * @param completedSegmentsDelta Completed segments delta to add
     * @param completedLessonsDelta  Completed lessons delta to add
     * @return Updated segments_completed count
     */
    private int updateStudyActivityAndReturnSegmentsCompleted(
            UUID userId,
            int studySecondsDelta,
            int completedSegmentsDelta,
            int completedLessonsDelta) {
        LocalDate today = today();
        LocalDateTime now = LocalDateTime.now(ZoneId.of(appSetting.getDefaultTimeZone()));
        if (supportsPostgresUpserts()) {
            return studyActivityRepository.upsertAndReturnSegmentsCompleted(
                    userId,
                    today,
                    studySecondsDelta,
                    completedSegmentsDelta,
                    completedLessonsDelta,
                    now);
        }

        StudyActivity activity = studyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .orElseGet(() -> StudyActivity.builder()
                        .userId(userId)
                        .activityDate(today)
                        .firstActivityAt(now)
                        .lastActivityAt(now)
                        .build());

        activity.setStudySeconds(activity.getStudySeconds() + studySecondsDelta);
        activity.setSegmentsCompleted(activity.getSegmentsCompleted() + completedSegmentsDelta);
        activity.setLessonsCompleted(activity.getLessonsCompleted() + completedLessonsDelta);
        activity.setLastActivityAt(now);
        if (activity.getId() == null) {
            return studyActivityRepository.save(activity).getSegmentsCompleted();
        }
        return activity.getSegmentsCompleted();
    }

    private boolean supportsPostgresUpserts() {
        Boolean cached = postgresUpsertsSupported;
        if (cached != null) {
            return cached;
        }
        boolean supported = true;
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                String productName = connection.getMetaData().getDatabaseProductName();
                supported = productName != null && productName.toLowerCase(Locale.ROOT).contains("postgresql");
            } catch (SQLException e) {
                supported = false;
            }
        }
        postgresUpsertsSupported = supported;
        return supported;
    }
}
