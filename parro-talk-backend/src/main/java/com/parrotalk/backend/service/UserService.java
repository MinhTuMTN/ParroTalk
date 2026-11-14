package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.CacheNames;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.dto.ChangePasswordRequest;
import com.parrotalk.backend.dto.UpdateUserProfileRequest;
import com.parrotalk.backend.dto.UserResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.AuthException;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.UserMapper;
import com.parrotalk.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{3,50}$");
    private static final Pattern DISALLOWED_URL_CHARS_PATTERN = Pattern.compile(".*[\\s\\x00-\\x1f\\x7f].*");
    private static final int MAX_FULL_NAME_LENGTH = 120;
    private static final int MAX_AVATAR_URL_LENGTH = 2048;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByEmailIncludeDeleted(String email) {
        return userRepository.findByEmailIncludeDeleted(email);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public User save(User user) {
        User saved = userRepository.save(user);
        evictUserCachesAfterCommit(saved.getEmail(), saved.getId());
        return saved;
    }

    public User saveAndFlush(User user) {
        User saved = userRepository.saveAndFlush(user);
        evictUserCachesAfterCommit(saved.getEmail(), saved.getId());
        return saved;
    }

    /**
     * Updates user's last active timestamp and saves to database while evicting relevant caches after commit.
     *
     * @param user User entity to update
     * @return Updated User entity
     */
    @Transactional
    public User updateLastActiveAt(User user) {
        user.setLastActiveAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        evictUserCachesAfterCommit(saved.getEmail(), saved.getId());
        return saved;
    }

    /**
     * Updates the authenticated user's profile (full name, optional display username, optional avatar URL),
     * loading a managed entity from the database and evicting both user and admin detail caches.
     */
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        if (userId == null) {
            throw new ParroTalkException("Authentication is required.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        if (request == null) {
            throw new ParroTalkException(
                    "Profile update request must not be null.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ParroTalkException("User not found.", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        String trimmedFullName = request.getFullName() == null ? "" : request.getFullName().trim();
        if (trimmedFullName.isEmpty() || trimmedFullName.length() > MAX_FULL_NAME_LENGTH) {
            throw new ParroTalkException(
                    "Full name must be between 1 and " + MAX_FULL_NAME_LENGTH + " characters.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST);
        }
        user.setFullName(trimmedFullName);

        if (request.getUsername() != null) {
            String normalizedUsername = request.getUsername().trim().toLowerCase(Locale.ROOT);
            if (normalizedUsername.isEmpty()) {
                throw new ParroTalkException(
                        "Username must not be blank when provided.",
                        ErrorCode.VALIDATION_ERROR.name(),
                        HttpStatus.BAD_REQUEST);
            }
            if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
                throw new ParroTalkException(
                        "Username must be 3-50 characters and contain only lowercase letters, numbers, dots, underscores, or hyphens.",
                        ErrorCode.VALIDATION_ERROR.name(),
                        HttpStatus.BAD_REQUEST);
            }
            userRepository.findByDisplayUsernameIgnoreCase(normalizedUsername).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new ParroTalkException(
                            "An account with this username already exists.",
                            "DUPLICATE_USERNAME",
                            HttpStatus.BAD_REQUEST);
                }
            });
            // Never call user.setUsername(...), which mutates User.email in UserDetails
            user.setDisplayUsername(normalizedUsername);
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizeAndValidateAvatarUrl(request.getAvatarUrl()));
        }

        User saved = userRepository.save(user);
        evictUserCachesAfterCommit(saved.getEmail(), saved.getId());
        return userMapper.toUserResponse(saved);
    }

    /**
     * Changes the user's password, or sets an initial local password for passwordless OAuth users.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        if (userId == null) {
            throw new ParroTalkException("Authentication is required.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        if (request == null) {
            throw new ParroTalkException(
                    "Password change request must not be null.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST);
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()
                || request.getNewPassword().length() < 6 || request.getNewPassword().length() > 100) {
            throw new ParroTalkException(
                    "New password must be between 6 and 100 characters.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST);
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_MISMATCH);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ParroTalkException("User not found.", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        boolean hasExistingPassword = user.getPassword() != null && !user.getPassword().isBlank();
        if (hasExistingPassword) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new ParroTalkException(
                        "Current password is required.",
                        ErrorCode.VALIDATION_ERROR.name(),
                        HttpStatus.BAD_REQUEST);
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new ParroTalkException(
                        "Current password is incorrect.",
                        ErrorCode.INVALID_CREDENTIALS.name(),
                        HttpStatus.BAD_REQUEST);
            }
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User saved = userRepository.save(user);
        evictUserCachesAfterCommit(saved.getEmail(), saved.getId());
    }

    private String normalizeAndValidateAvatarUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > MAX_AVATAR_URL_LENGTH || DISALLOWED_URL_CHARS_PATTERN.matcher(trimmed).matches()) {
            throw new ParroTalkException(
                    "Avatar URL is invalid.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST);
        }

        if (trimmed.startsWith("/")) {
            if (trimmed.startsWith("//")) {
                throw new ParroTalkException(
                        "Avatar URL must be a valid http(s) URL or relative path.",
                        ErrorCode.VALIDATION_ERROR.name(),
                        HttpStatus.BAD_REQUEST);
            }
            parseUriOrThrow(trimmed);
            return trimmed;
        }

        URI uri = parseUriOrThrow(trimmed);
        String scheme = uri.getScheme();
        boolean validHttpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        if (!validHttpScheme || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ParroTalkException(
                    "Avatar URL must use http://, https://, or a relative path starting with '/'.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private URI parseUriOrThrow(String value) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new ParroTalkException(
                    "Avatar URL is malformed.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void evictUserCachesAfterCommit(String email, UUID userId) {
        if (cacheManager == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictUserCachesNow(email, userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictUserCachesNow(email, userId);
            }
        });
    }

    private void evictUserCachesNow(String email, UUID userId) {
        if (email != null && !email.isBlank()) {
            evictCacheEntry(CacheNames.USER_CACHE, email);
            String lowerEmail = email.toLowerCase(Locale.ROOT);
            if (!lowerEmail.equals(email)) {
                evictCacheEntry(CacheNames.USER_CACHE, lowerEmail);
            }
        }
        if (userId != null) {
            evictCacheEntry(CacheNames.ADMIN_USER_DETAIL_CACHE, userId);
        }
    }

    private void evictCacheEntry(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
            }
        } catch (Exception e) {
            log.warn("Failed to evict key '{}' from cache '{}': {}", key, cacheName, e.getMessage());
        }
    }
}
