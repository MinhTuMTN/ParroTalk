package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.ChangePasswordRequest;
import com.parrotalk.backend.dto.UpdateUserProfileRequest;
import com.parrotalk.backend.dto.UpdateUserSettingsRequest;
import com.parrotalk.backend.dto.UserProfileResponse;
import com.parrotalk.backend.dto.UserResponse;
import com.parrotalk.backend.dto.UserSettingsDto;
import com.parrotalk.backend.dto.UserStreakResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserActiveDay;
import com.parrotalk.backend.entity.UserProgress;
import com.parrotalk.backend.entity.UserStreak;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.UserMapper;
import com.parrotalk.backend.repository.UserActiveDayRepository;
import com.parrotalk.backend.repository.UserProgressRepository;
import com.parrotalk.backend.repository.UserStreakRepository;
import com.parrotalk.backend.service.UserService;
import com.parrotalk.backend.service.UserSettingsService;
import com.parrotalk.backend.service.UserStreakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProgressRepository progressRepository;
    private final UserStreakRepository streakRepository;
    private final UserActiveDayRepository activeDayRepository;
    private final UserStreakService userStreakService;
    private final UserSettingsService userSettingsService;
    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal User principal) {
        UUID userId = requireUserId(principal);
        User currentUser = userService.findById(userId).orElse(principal);

        UserProgress progress = progressRepository.findById(userId)
                .orElse(UserProgress.builder().build());

        UserStreak streak = streakRepository.findById(userId)
                .orElse(UserStreak.builder().build());

        List<LocalDate> activeDays = activeDayRepository.findAllByUserIdOrderByActiveDateDesc(userId)
                .stream()
                .map(UserActiveDay::getActiveDate)
                .collect(Collectors.toList());

        UserProfileResponse response = UserProfileResponse.builder()
                .id(currentUser.getId())
                .fullName(currentUser.getFullName())
                .username(userMapper.resolveUsername(currentUser))
                .email(currentUser.getEmail())
                .avatarUrl(currentUser.getAvatarUrl())
                .hasPassword(userMapper.resolveHasPassword(currentUser))
                .totalLessonsCompleted(progress.getTotalLessonsCompleted() != null ? progress.getTotalLessonsCompleted() : 0)
                .totalScore(progress.getTotalScore() != null ? progress.getTotalScore() : 0.0)
                .avgScore(progress.getAvgScore() != null ? progress.getAvgScore() : 0.0)
                .currentStreak(streak.getCurrentStreak() != null ? streak.getCurrentStreak() : 0)
                .longestStreak(streak.getLongestStreak() != null ? streak.getLongestStreak() : 0)
                .activeDays(activeDays)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/streak")
    public ResponseEntity<UserStreakResponse> getMyStreak(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(userStreakService.getUserStreak(requireUserId(user), year));
    }

    @GetMapping("/me/settings")
    public ResponseEntity<UserSettingsDto> getMySettings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userSettingsService.getSettings(requireUserId(user)));
    }

    @PutMapping("/me/settings")
    public ResponseEntity<UserSettingsDto> updateMySettings(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateUserSettingsRequest request) {
        return ResponseEntity.ok(userSettingsService.updateSettings(requireUserId(user), request));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(requireUserId(user), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(requireUserId(user), request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Password updated successfully.")
                .build());
    }

    private UUID requireUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new ParroTalkException("Authentication is required.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        return user.getId();
    }
}
