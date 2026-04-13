package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.Difficulty;
import com.parrotalk.backend.dto.SegmentResultRequest;
import com.parrotalk.backend.dto.SubmitLessonRequest;
import com.parrotalk.backend.dto.SubmitLessonResponse;
import com.parrotalk.backend.entity.*;
import com.parrotalk.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningService {

    private final UserLessonResultRepository resultRepository;
    private final UserProgressRepository progressRepository;
    private final UserStreakRepository streakRepository;
    private final UserActiveDayRepository activeDayRepository;
    private final TranscriptionSegmentRepository segmentRepository;

    @Transactional
    public SubmitLessonResponse submitLessonProgress(UUID userId, UUID lessonId, SubmitLessonRequest request) {
        double totalScore = 0.0;
        int totalHintWords = 0;
        int totalReplayCount = 0;
        int totalAttempts = 0;

        List<SegmentResultRequest> segmentResults = request.getSegmentResults();
        if (segmentResults == null || segmentResults.isEmpty()) {
            return SubmitLessonResponse.builder().score(0.0).passed(false).build();
        }

        for (SegmentResultRequest sr : segmentResults) {
            totalHintWords += sr.getHintWords();
            totalReplayCount += sr.getReplayCount();
            totalAttempts += sr.getAttempts();

            TranscriptionSegment segment = segmentRepository.findById(sr.getSegmentId())
                    .orElseThrow(() -> new RuntimeException("Segment not found"));

            Difficulty diff = segment.getDifficulty();
            if (diff == null) {
                diff = Difficulty.MEDIUM; // Default fallback
            }

            int free = 2;
            int step = 2;
            switch (diff) {
                case SHORT:
                    free = 1; step = 2; break;
                case MEDIUM:
                    free = 2; step = 2; break;
                case LONG:
                    free = 3; step = 3; break;
            }

            int penalty = 0;
            if (sr.getReplayCount() > free) {
                penalty = (int) Math.floor((double) (sr.getReplayCount() - free) / step) + 1;
            }

            double segmentScore = Math.max(4.0, 10.0 - Math.ceil(sr.getHintWords() / 2.0) - penalty);
            totalScore += segmentScore;
        }

        double averageScore = totalScore / segmentResults.size();
        boolean passed = averageScore >= 6.0;

        UserLessonResult lessonResult = resultRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElse(UserLessonResult.builder()
                        .userId(userId)
                        .lessonId(lessonId)
                        .build());

        lessonResult.setScore(averageScore);
        lessonResult.setHintWords(totalHintWords);
        lessonResult.setReplayCount(totalReplayCount);
        lessonResult.setAttempts(totalAttempts);
        lessonResult.setIsPassed(passed);
        resultRepository.save(lessonResult);

        if (request.isFinished()) {
            updateUserProgress(userId, averageScore);
            updateUserStreak(userId);
        }

        return SubmitLessonResponse.builder()
                .score(averageScore)
                .passed(passed)
                .build();
    }

    private void updateUserProgress(UUID userId, double newScore) {
        UserProgress progress = progressRepository.findById(userId)
                .orElse(UserProgress.builder()
                        .userId(userId)
                        .build());

        int count = progress.getTotalLessonsCompleted();
        double total = progress.getTotalScore();
        progress.setTotalLessonsCompleted(count + 1);
        progress.setTotalScore(total + newScore);
        progress.setAvgScore((total + newScore) / (count + 1));
        progress.setLastActivityDate(LocalDateTime.now());
        progressRepository.save(progress);
    }

    private void updateUserStreak(UUID userId) {
        LocalDate today = LocalDate.now();
        
        Optional<UserActiveDay> todayActive = activeDayRepository.findByUserIdAndActiveDate(userId, today);
        if (todayActive.isEmpty()) {
            activeDayRepository.save(UserActiveDay.builder().userId(userId).activeDate(today).build());
        }

        UserStreak streak = streakRepository.findById(userId)
                .orElse(UserStreak.builder().userId(userId).build());

        LocalDate lastActive = streak.getLastActiveDate();
        if (lastActive == null) {
            streak.setCurrentStreak(1);
            streak.setLongestStreak(1);
            streak.setLastActiveDate(today);
        } else if (!lastActive.equals(today)) {
            if (lastActive.plusDays(1).equals(today)) {
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                    streak.setLongestStreak(streak.getCurrentStreak());
                }
            } else {
                streak.setCurrentStreak(1);
            }
            streak.setLastActiveDate(today);
        }
        streakRepository.save(streak);
    }
}
