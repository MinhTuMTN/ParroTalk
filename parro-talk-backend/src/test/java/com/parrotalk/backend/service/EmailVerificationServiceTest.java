package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.parrotalk.backend.config.EmailVerificationProperties;
import com.parrotalk.backend.config.FrontendProperties;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.AuthException;

class EmailVerificationServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private UserService userService;
    private VerifyEmailProducer verifyEmailProducer;
    private EmailVerificationService emailVerificationService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        hashOperations = mock(HashOperations.class);
        userService = mock(UserService.class);
        verifyEmailProducer = mock(VerifyEmailProducer.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        EmailVerificationProperties properties = new EmailVerificationProperties();
        properties.setTokenExpirationMinutes(30);
        properties.setResendCooldownSeconds(60);
        properties.setMetadataRetentionMinutes(1440);

        FrontendProperties frontendProperties = new FrontendProperties();
        frontendProperties.setBaseUrl("http://localhost:3000");

        emailVerificationService = new EmailVerificationService(
                redisTemplate,
                properties,
                frontendProperties,
                userService,
                verifyEmailProducer);
    }

    @Test
    void verifyEmailMarksValidTokenAsUsedAndVerifiesUser() throws Exception {
        String rawToken = "valid-token";
        String tokenHash = hash(rawToken);
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .fullName("User")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(false)
                .build();

        when(hashOperations.entries("email_verify_meta:" + tokenHash)).thenReturn(Map.of(
                "userId", userId.toString(),
                "expiresAt", Long.toString(Instant.now().plusSeconds(300).getEpochSecond()),
                "usedAt", "",
                "invalidatedAt", ""));
        when(valueOperations.get("email_verify_latest:" + userId)).thenReturn(tokenHash);
        when(valueOperations.get("email_verify:" + tokenHash)).thenReturn(userId.toString());
        when(userService.findById(userId)).thenReturn(Optional.of(user));

        String message = emailVerificationService.verifyEmail(rawToken);

        assertEquals("Email verified successfully", message);
        verify(hashOperations).put(
                org.mockito.ArgumentMatchers.eq("email_verify_meta:" + tokenHash),
                org.mockito.ArgumentMatchers.eq("usedAt"),
                org.mockito.ArgumentMatchers.anyString());
        verify(userService).save(user);
    }

    @Test
    void verifyEmailRejectsExpiredToken() throws Exception {
        String rawToken = "expired-token";
        String tokenHash = hash(rawToken);
        when(hashOperations.entries("email_verify_meta:" + tokenHash)).thenReturn(Map.of(
                "userId", UUID.randomUUID().toString(),
                "expiresAt", Long.toString(Instant.now().minusSeconds(60).getEpochSecond()),
                "usedAt", "",
                "invalidatedAt", ""));

        AuthException exception = assertThrows(AuthException.class,
                () -> emailVerificationService.verifyEmail(rawToken));

        assertEquals(ErrorCode.VERIFICATION_TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    void verifyEmailRejectsUsedToken() throws Exception {
        String rawToken = "used-token";
        String tokenHash = hash(rawToken);
        when(hashOperations.entries("email_verify_meta:" + tokenHash)).thenReturn(Map.of(
                "userId", UUID.randomUUID().toString(),
                "expiresAt", Long.toString(Instant.now().plusSeconds(60).getEpochSecond()),
                "usedAt", Long.toString(Instant.now().getEpochSecond()),
                "invalidatedAt", ""));

        AuthException exception = assertThrows(AuthException.class,
                () -> emailVerificationService.verifyEmail(rawToken));

        assertEquals(ErrorCode.VERIFICATION_TOKEN_USED, exception.getErrorCode());
    }

    @Test
    void resendVerificationEmailReturnsGenericMessageForUnknownEmail() {
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());

        String message = emailVerificationService.resendVerificationEmail("missing@example.com");

        assertEquals("If an account exists for that email, a verification email has been sent.", message);
    }

    @Test
    void resendVerificationEmailRejectsRapidRepeatRequests() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("pending@example.com")
                .fullName("Pending User")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(false)
                .build();

        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(valueOperations.setIfAbsent(
                org.mockito.ArgumentMatchers.eq("email_verify_resend_cooldown:" + userId),
                org.mockito.ArgumentMatchers.eq("1"),
                org.mockito.ArgumentMatchers.any())).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class,
                () -> emailVerificationService.resendVerificationEmail(user.getEmail()));

        assertEquals(ErrorCode.RESEND_COOLDOWN_ACTIVE, exception.getErrorCode());
    }

    private String hash(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}
