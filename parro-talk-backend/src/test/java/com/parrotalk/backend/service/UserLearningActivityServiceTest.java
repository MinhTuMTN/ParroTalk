package com.parrotalk.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.parrotalk.backend.config.AppSetting;
import com.parrotalk.backend.entity.UserStreak;
import com.parrotalk.backend.repository.StudyActivityRepository;
import com.parrotalk.backend.repository.UserActiveDayRepository;
import com.parrotalk.backend.repository.UserProgressRepository;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.repository.UserStreakRepository;

@ExtendWith(MockitoExtension.class)
class UserLearningActivityServiceTest {

    @Mock
    private UserProgressRepository progressRepository;

    @Mock
    private UserStreakRepository streakRepository;

    @Mock
    private UserActiveDayRepository activeDayRepository;

    @Mock
    private StudyActivityRepository studyActivityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppSetting appSetting;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserLearningActivityService learningActivityService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(learningActivityService, "redisTemplate", redisTemplate);
        when(appSetting.getDefaultTimeZone()).thenReturn("UTC");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void recordSegmentAttempt_crossingThresholdFromOneToTwo_recordsStudyDayAndStreak() {
        when(studyActivityRepository.upsertAndReturnSegmentsCompleted(
                eq(userId),
                any(LocalDate.class),
                eq(15),
                eq(1),
                eq(0),
                any(LocalDateTime.class)))
                .thenReturn(1);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.increment(anyString())).thenReturn(2L);
        when(activeDayRepository.insertIfAbsent(eq(userId), any(LocalDate.class))).thenReturn(1);
        when(streakRepository.findById(userId)).thenReturn(Optional.empty());

        learningActivityService.recordSegmentAttempt(userId, true, 15);

        verify(studyActivityRepository).upsertAndReturnSegmentsCompleted(
                eq(userId),
                any(LocalDate.class),
                eq(15),
                eq(1),
                eq(0),
                any(LocalDateTime.class));
        verify(activeDayRepository).insertIfAbsent(eq(userId), any(LocalDate.class));
        verify(streakRepository).findById(userId);
        verify(streakRepository).save(any(UserStreak.class));
        verify(valueOperations).set(anyString(), eq("1"), any(Duration.class));
    }

    @Test
    void recordSegmentAttempt_alreadyUpdatedStreakTodayInRedis_skipsStudyDayAndStreak() {
        when(studyActivityRepository.upsertAndReturnSegmentsCompleted(
                eq(userId),
                any(LocalDate.class),
                eq(10),
                eq(1),
                eq(0),
                any(LocalDateTime.class)))
                .thenReturn(3);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        learningActivityService.recordSegmentAttempt(userId, true, 10);

        verify(valueOperations, never()).increment(anyString());
        verifyNoInteractions(activeDayRepository, streakRepository);
    }

    @Test
    void recordSegmentAttempt_wrongAttemptAboveThreshold_skipsStudyDayAndStreak() {
        when(studyActivityRepository.upsertAndReturnSegmentsCompleted(
                eq(userId),
                any(LocalDate.class),
                eq(10),
                eq(0),
                eq(0),
                any(LocalDateTime.class)))
                .thenReturn(2);

        learningActivityService.recordSegmentAttempt(userId, false, 10);

        verifyNoInteractions(redisTemplate, activeDayRepository, streakRepository);
    }

    @Test
    void recordSegmentAttempt_newStudyActivityOnFirstSegment_skipsStudyDay() {
        when(studyActivityRepository.upsertAndReturnSegmentsCompleted(
                eq(userId),
                any(LocalDate.class),
                eq(12),
                eq(1),
                eq(0),
                any(LocalDateTime.class)))
                .thenReturn(1);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        learningActivityService.recordSegmentAttempt(userId, true, 12);

        verify(redisTemplate).expire(anyString(), any(Duration.class));
        verifyNoInteractions(activeDayRepository, streakRepository);
    }

    @Test
    void recordCompletedLesson_usesNativeUpsertsAndTargetedUserUpdate() {
        when(progressRepository.upsertLessonCompletion(eq(userId), eq(95.0), any(LocalDateTime.class)))
                .thenReturn(1);
        when(userRepository.updateLastActiveAt(eq(userId), any(LocalDateTime.class))).thenReturn(1);
        when(studyActivityRepository.upsertAndReturnSegmentsCompleted(
                eq(userId),
                any(LocalDate.class),
                eq(0),
                eq(0),
                eq(1),
                any(LocalDateTime.class)))
                .thenReturn(2);
        when(activeDayRepository.insertIfAbsent(eq(userId), any(LocalDate.class))).thenReturn(1);
        when(streakRepository.findById(userId)).thenReturn(Optional.empty());

        learningActivityService.recordCompletedLesson(userId, 95.0);

        verify(progressRepository).upsertLessonCompletion(eq(userId), eq(95.0), any(LocalDateTime.class));
        verify(progressRepository, never()).findById(any());
        verify(userRepository).updateLastActiveAt(eq(userId), any(LocalDateTime.class));
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
        verify(studyActivityRepository).upsertAndReturnSegmentsCompleted(
                eq(userId),
                any(LocalDate.class),
                eq(0),
                eq(0),
                eq(1),
                any(LocalDateTime.class));
        verify(activeDayRepository).insertIfAbsent(eq(userId), any(LocalDate.class));
        verify(streakRepository).findById(userId);
        verify(streakRepository).save(any(UserStreak.class));
    }
}
