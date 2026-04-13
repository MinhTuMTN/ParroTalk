package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.UserProfileResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserActiveDay;
import com.parrotalk.backend.entity.UserProgress;
import com.parrotalk.backend.entity.UserStreak;
import com.parrotalk.backend.repository.UserActiveDayRepository;
import com.parrotalk.backend.repository.UserProgressRepository;
import com.parrotalk.backend.repository.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProgressRepository progressRepository;
    private final UserStreakRepository streakRepository;
    private final UserActiveDayRepository activeDayRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal User user) {
        UserProgress progress = progressRepository.findById(user.getId())
                .orElse(UserProgress.builder().build());

        UserStreak streak = streakRepository.findById(user.getId())
                .orElse(UserStreak.builder().build());

        List<LocalDate> activeDays = activeDayRepository.findAllByUserIdOrderByActiveDateDesc(user.getId())
                .stream()
                .map(UserActiveDay::getActiveDate)
                .collect(Collectors.toList());

        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .totalLessonsCompleted(progress.getTotalLessonsCompleted() != null ? progress.getTotalLessonsCompleted() : 0)
                .totalScore(progress.getTotalScore() != null ? progress.getTotalScore() : 0.0)
                .avgScore(progress.getAvgScore() != null ? progress.getAvgScore() : 0.0)
                .currentStreak(streak.getCurrentStreak() != null ? streak.getCurrentStreak() : 0)
                .longestStreak(streak.getLongestStreak() != null ? streak.getLongestStreak() : 0)
                .activeDays(activeDays)
                .build();

        return ResponseEntity.ok(response);
    }
}
