package com.parrotalk.backend.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.UserStatus;
import com.parrotalk.backend.dto.AdminResetPasswordResponse;
import com.parrotalk.backend.dto.AdminUserResponse;
import com.parrotalk.backend.dto.CreateUserRequest;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.UpdateUserRequest;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserProgress;
import com.parrotalk.backend.entity.UserStreak;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.UserProgressRepository;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.repository.UserStreakRepository;
import com.parrotalk.backend.specification.UserSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();

    private final UserRepository userRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserStreakRepository userStreakRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<AdminUserResponse> searchUsers(
            String search,
            Role role,
            UserStatus status,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<User> specification = Specification
                .where(UserSpecification.matchesSearch(search))
                .and(UserSpecification.hasRole(role))
                .and(UserSpecification.hasStatus(status));

        Page<AdminUserResponse> userPage = userRepository.findAll(specification, pageable)
                .map(this::toResponse);

        return PageResponse.<AdminUserResponse>builder()
                .content(userPage.getContent())
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    public AdminUserResponse getUserDetail(UUID id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public AdminUserResponse createUser(CreateUserRequest request) {
        ensureEmailAvailable(request.getEmail(), null);
        ensureUsernameAvailable(request.getUsername(), null);

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .displayUsername(request.getUsername().trim())
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(request.getStatus() == UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findUser(id);
        ensureEmailAvailable(request.getEmail(), id);
        ensureUsernameAvailable(request.getUsername(), id);

        user.setFullName(request.getFullName().trim());
        user.setDisplayUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setRole(request.getRole());
        user.setEnabled(request.getStatus() == UserStatus.ACTIVE);

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID id, UserStatus status, UUID currentAdminId) {
        User user = findUser(id);
        if (user.getId().equals(currentAdminId) && status == UserStatus.INACTIVE) {
            throw new ParroTalkException(
                    "You cannot deactivate your own account.",
                    "CANNOT_DEACTIVATE_SELF",
                    HttpStatus.BAD_REQUEST);
        }

        user.setEnabled(status == UserStatus.ACTIVE);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminResetPasswordResponse resetPassword(UUID id) {
        User user = findUser(id);
        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);

        return AdminResetPasswordResponse.builder()
                .temporaryPassword(temporaryPassword)
                .build();
    }

    @Transactional
    public void deleteUser(UUID id, UUID currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new ParroTalkException(
                    "You cannot delete your own account.",
                    "CANNOT_DELETE_SELF",
                    HttpStatus.BAD_REQUEST);
        }

        userRepository.delete(findUser(id));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ParroTalkException(
                        "User not found.",
                        "USER_NOT_FOUND",
                        HttpStatus.NOT_FOUND));
    }

    private void ensureEmailAvailable(String email, UUID currentUserId) {
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new ParroTalkException(
                        "An account with this email already exists.",
                        "DUPLICATE_EMAIL",
                        HttpStatus.BAD_REQUEST);
            }
        });
    }

    private void ensureUsernameAvailable(String username, UUID currentUserId) {
        userRepository.findByDisplayUsernameIgnoreCase(username.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new ParroTalkException(
                        "An account with this username already exists.",
                        "DUPLICATE_USERNAME",
                        HttpStatus.BAD_REQUEST);
            }
        });
    }

    private AdminUserResponse toResponse(User user) {
        UserProgress progress = userProgressRepository.findById(user.getId()).orElse(null);
        UserStreak streak = userStreakRepository.findById(user.getId()).orElse(null);

        LocalDateTime lastActiveAt = progress != null ? progress.getLastActivityDate() : null;

        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(resolveDisplayUsername(user))
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.isEnabled() ? UserStatus.ACTIVE : UserStatus.INACTIVE)
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastActiveAt(lastActiveAt)
                .totalLessonsCompleted(progress != null && progress.getTotalLessonsCompleted() != null
                        ? progress.getTotalLessonsCompleted()
                        : 0)
                .totalScore(progress != null && progress.getTotalScore() != null ? progress.getTotalScore() : 0.0)
                .avgScore(progress != null && progress.getAvgScore() != null ? progress.getAvgScore() : 0.0)
                .currentStreak(streak != null && streak.getCurrentStreak() != null ? streak.getCurrentStreak() : 0)
                .longestStreak(streak != null && streak.getLongestStreak() != null ? streak.getLongestStreak() : 0)
                .build();
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        for (int index = 0; index < 12; index++) {
            password.append(TEMP_PASSWORD_CHARS[SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length)]);
        }
        return password.toString();
    }

    private String resolveDisplayUsername(User user) {
        if (user.getDisplayUsername() != null && !user.getDisplayUsername().isBlank()) {
            return user.getDisplayUsername();
        }

        return user.getEmail().substring(0, user.getEmail().indexOf("@"));
    }
}
