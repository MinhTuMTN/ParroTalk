package com.parrotalk.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private UUID id;
    private String fullName;
    private String username;
    private String email;
    private Role role;
    private UserStatus status;
    private String avatarUrl;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private int totalLessonsCompleted;
    private double totalScore;
    private double avgScore;
    private int currentStreak;
    private int longestStreak;
}
