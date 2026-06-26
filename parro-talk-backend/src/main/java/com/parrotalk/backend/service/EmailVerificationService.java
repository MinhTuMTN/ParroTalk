package com.parrotalk.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.config.EmailVerificationProperties;
import com.parrotalk.backend.config.FrontendProperties;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.dto.VerifyEmailMessage;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.AuthException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final EmailVerificationProperties properties;
    private final FrontendProperties frontendProperties;
    private final UserService userService;
    private final VerifyEmailProducer verifyEmailProducer;

    @Transactional
    public void issueVerificationEmail(User user) {
        String token = generateToken();
        String tokenHash = hashToken(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(properties.getTokenExpirationMinutes());

        invalidateLatestToken(user.getId());
        storeToken(user.getId(), tokenHash, expiresAt);

        VerifyEmailMessage message = new VerifyEmailMessage(
                user.getEmail(),
                user.getFullName(),
                buildVerificationUrl(token),
                properties.getTokenExpirationMinutes(),
                0);

        try {
            verifyEmailProducer.enqueue(message);
        } catch (Exception e) {
            log.error("Failed to enqueue verification email for {}", user.getEmail(), e);
        }
    }

    @Transactional
    public String verifyEmail(String rawToken) {
        String tokenHash = hashToken(rawToken);
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        Map<Object, Object> metadata = hashOps.entries(metaKey(tokenHash));

        if (metadata.isEmpty()) {
            throw new AuthException(ErrorCode.VERIFICATION_TOKEN_INVALID);
        }

        if (hasValue(metadata.get("usedAt"))) {
            throw new AuthException(ErrorCode.VERIFICATION_TOKEN_USED);
        }

        if (hasValue(metadata.get("invalidatedAt"))) {
            throw new AuthException(ErrorCode.VERIFICATION_TOKEN_SUPERSEDED);
        }

        long expiresAtEpoch = Long.parseLong(String.valueOf(metadata.get("expiresAt")));
        if (Instant.now().getEpochSecond() > expiresAtEpoch) {
            throw new AuthException(ErrorCode.VERIFICATION_TOKEN_EXPIRED);
        }

        UUID userId = UUID.fromString(String.valueOf(metadata.get("userId")));
        String latestTokenHash = redisTemplate.opsForValue().get(latestKey(userId));
        String activeUserId = redisTemplate.opsForValue().get(activeKey(tokenHash));
        if (!tokenHash.equals(latestTokenHash) || !userId.toString().equals(activeUserId)) {
            throw new AuthException(ErrorCode.VERIFICATION_TOKEN_SUPERSEDED);
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.VERIFICATION_TOKEN_INVALID));

        markTokenUsed(tokenHash);
        redisTemplate.delete(activeKey(tokenHash));
        redisTemplate.delete(latestKey(userId));

        if (user.isEmailVerified()) {
            return "Email is already verified.";
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userService.save(user);
        return "Email verified successfully";
    }

    @Transactional
    public String resendVerificationEmail(String email) {
        User user = userService.findByEmail(email).orElse(null);
        if (user == null) {
            return genericResendMessage();
        }

        if (user.isEmailVerified()) {
            return "Email is already verified.";
        }

        Boolean cooldownStarted = redisTemplate.opsForValue().setIfAbsent(
                cooldownKey(user.getId()),
                "1",
                Duration.ofSeconds(properties.getResendCooldownSeconds()));
        if (!Boolean.TRUE.equals(cooldownStarted)) {
            throw new AuthException(ErrorCode.RESEND_COOLDOWN_ACTIVE);
        }

        issueVerificationEmail(user);
        return genericResendMessage();
    }

    private void storeToken(UUID userId, String tokenHash, LocalDateTime expiresAt) {
        Duration tokenTtl = Duration.ofMinutes(properties.getTokenExpirationMinutes());
        Duration metadataTtl = Duration.ofMinutes(properties.getMetadataRetentionMinutes());
        redisTemplate.opsForValue().set(activeKey(tokenHash), userId.toString(), tokenTtl);
        redisTemplate.opsForValue().set(latestKey(userId), tokenHash, tokenTtl);

        redisTemplate.opsForHash().putAll(metaKey(tokenHash), Map.of(
                "userId", userId.toString(),
                "expiresAt", Long.toString(expiresAt.toEpochSecond(java.time.ZoneOffset.UTC)),
                "usedAt", "",
                "invalidatedAt", ""));
        redisTemplate.expire(metaKey(tokenHash), metadataTtl);
    }

    private void invalidateLatestToken(UUID userId) {
        String previousTokenHash = redisTemplate.opsForValue().get(latestKey(userId));
        if (previousTokenHash == null) {
            return;
        }

        redisTemplate.opsForHash().put(
                metaKey(previousTokenHash),
                "invalidatedAt",
                Long.toString(Instant.now().getEpochSecond()));
        redisTemplate.delete(activeKey(previousTokenHash));
    }

    private void markTokenUsed(String tokenHash) {
        redisTemplate.opsForHash().put(
                metaKey(tokenHash),
                "usedAt",
                Long.toString(Instant.now().getEpochSecond()));
    }

    private String buildVerificationUrl(String token) {
        return frontendProperties.getBaseUrl() + "/verify-email?token=" + token;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private boolean hasValue(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private String activeKey(String tokenHash) {
        return "email_verify:" + tokenHash;
    }

    private String latestKey(UUID userId) {
        return "email_verify_latest:" + userId;
    }

    private String metaKey(String tokenHash) {
        return "email_verify_meta:" + tokenHash;
    }

    private String cooldownKey(UUID userId) {
        return "email_verify_resend_cooldown:" + userId;
    }

    private String genericResendMessage() {
        return "If an account exists for that email, a verification email has been sent.";
    }
}
