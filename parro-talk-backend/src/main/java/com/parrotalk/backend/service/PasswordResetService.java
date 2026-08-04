package com.parrotalk.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.config.FrontendProperties;
import com.parrotalk.backend.config.PasswordResetProperties;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.dto.ResetPasswordRequest;
import com.parrotalk.backend.dto.TokenVerificationResponse;
import com.parrotalk.backend.entity.PasswordResetToken;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.AuthException;
import com.parrotalk.backend.repository.PasswordResetTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the forgot-password / reset-password flow.
 *
 * <ul>
 *   <li>Generates cryptographically-secure tokens (32 bytes, Base64-URL).</li>
 *   <li>Stores only the SHA-256 hash in the database.</li>
 *   <li>Invalidates all previous active tokens when a new one is issued.</li>
 * </ul>
 *
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetProperties properties;
    private final FrontendProperties frontendProperties;

    // ---- Public API ----------------------------------------------------------

    /**
     * Processes a forgot-password request.
     * <p>
     * If the email exists, a reset token is generated, persisted (hashed), and
     * an email is dispatched. If the email does not exist, the method silently
     * succeeds to prevent email enumeration.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userService.findByEmail(email.trim().toLowerCase());

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent account");
            return;
        }

        User user = userOpt.get();

        // Invalidate any previously-issued active tokens for this user
        int invalidated = tokenRepository.invalidateAllActiveTokensForUser(user.getId());
        if (invalidated > 0) {
            log.info("Invalidated {} previous reset token(s) for user", invalidated);
        }

        // Generate and persist a new token
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(properties.getTokenExpirationMinutes());

        PasswordResetToken entity = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiredAt(expiresAt)
                .createdBy("system")
                .build();

        tokenRepository.save(entity);
        log.info("Password reset token issued for user");

        // Send the email with the raw token
        String resetUrl = buildResetUrl(rawToken);
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetUrl, user.getFullName());
            log.info("Password reset email dispatched");
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            // Swallow the exception – the user still gets a 200 OK
        }
    }

    /**
     * Verifies whether a raw token is still valid.
     *
     * @return a {@link TokenVerificationResponse} with status
     *         {@code "valid"}, {@code "expired"}, {@code "used"}, or {@code "invalid"}.
     */
    @Transactional(readOnly = true)
    public TokenVerificationResponse verifyToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            log.info("Token verification: invalid token");
            return TokenVerificationResponse.builder().status("invalid").build();
        }

        PasswordResetToken token = tokenOpt.get();

        if (token.isUsed()) {
            log.info("Token verification: token already used");
            return TokenVerificationResponse.builder().status("used").build();
        }

        if (token.isExpired()) {
            log.info("Token verification: token expired");
            return TokenVerificationResponse.builder().status("expired").build();
        }

        return TokenVerificationResponse.builder().status("valid").build();
    }

    /**
     * Resets the user's password using a valid reset token.
     *
     * @throws AuthException if the token is invalid, expired, or already used,
     *                       or if the passwords do not match
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_MISMATCH);
        }

        String tokenHash = hashToken(request.getToken());
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Password reset attempt with invalid token");
                    return new AuthException(ErrorCode.RESET_TOKEN_INVALID);
                });

        if (token.isUsed()) {
            log.warn("Password reset attempt with already-used token");
            throw new AuthException(ErrorCode.RESET_TOKEN_USED);
        }

        if (token.isExpired()) {
            log.warn("Password reset attempt with expired token");
            throw new AuthException(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        // Update the user's password
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.save(user);

        // Mark the token as used
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        log.info("Password reset successful for user");
    }

    /**
     * Deletes all tokens that expired more than 24 hours ago.
     *
     * @return the number of deleted tokens
     */
    @Transactional
    public int cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        int deleted = tokenRepository.deleteExpiredTokensBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired password reset token(s)", deleted);
        }
        return deleted;
    }

    // ---- Internals -----------------------------------------------------------

    /**
     * Generates a 32-byte cryptographically-secure random token,
     * Base64-URL-encoded without padding.
     */
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hashes a raw token with SHA-256 and returns the hex digest. */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String buildResetUrl(String rawToken) {
        return frontendProperties.getBaseUrl() + "/reset-password?token=" + rawToken;
    }
}
