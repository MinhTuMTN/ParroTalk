package com.parrotalk.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.parrotalk.backend.config.FrontendProperties;
import com.parrotalk.backend.config.PasswordResetProperties;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.dto.ResetPasswordRequest;
import com.parrotalk.backend.dto.TokenVerificationResponse;
import com.parrotalk.backend.entity.PasswordResetToken;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.AuthException;
import com.parrotalk.backend.repository.PasswordResetTokenRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserService userService;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetProperties properties;
    @Mock
    private FrontendProperties frontendProperties;

    @InjectMocks
    private PasswordResetService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .fullName("Test User")
                .password("oldEncodedPassword")
                .build();
    }

    // ---- requestPasswordReset ----

    @Nested
    @DisplayName("requestPasswordReset")
    class RequestPasswordReset {

        @Test
        @DisplayName("should generate token, invalidate old ones, and send email for existing user")
        void happyPath() {
            when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(properties.getTokenExpirationMinutes()).thenReturn(30L);
            when(frontendProperties.getBaseUrl()).thenReturn("https://parrotalk.fun");
            when(tokenRepository.invalidateAllActiveTokensForUser(testUser.getId())).thenReturn(1);

            service.requestPasswordReset("test@example.com");

            // Verify token was saved
            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());

            PasswordResetToken saved = captor.getValue();
            assertThat(saved.getTokenHash()).isNotBlank();
            assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex = 64 chars
            assertThat(saved.getUser()).isEqualTo(testUser);
            assertThat(saved.getExpiredAt()).isAfter(LocalDateTime.now().plusMinutes(29));
            assertThat(saved.getCreatedBy()).isEqualTo("system");

            // Verify old tokens were invalidated
            verify(tokenRepository).invalidateAllActiveTokensForUser(testUser.getId());

            // Verify email was sent
            verify(emailService).sendPasswordResetEmail(
                    eq("test@example.com"),
                    anyString(),
                    eq("Test User"));
        }

        @Test
        @DisplayName("should NOT throw for non-existent email (prevents enumeration)")
        void nonExistentEmail() {
            when(userService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // Should not throw
            service.requestPasswordReset("unknown@example.com");

            // Should NOT save any token or send email
            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should still return normally even if email sending fails")
        void emailFailure() {
            when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(properties.getTokenExpirationMinutes()).thenReturn(30L);
            when(frontendProperties.getBaseUrl()).thenReturn("https://parrotalk.fun");

            // Simulate email failure
            org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))
                    .when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());

            // Should not throw
            service.requestPasswordReset("test@example.com");

            // Token should still be persisted
            verify(tokenRepository).save(any(PasswordResetToken.class));
        }
    }

    // ---- verifyToken ----

    @Nested
    @DisplayName("verifyToken")
    class VerifyToken {

        @Test
        @DisplayName("should return 'valid' for a fresh, unexpired token")
        void validToken() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash("somehash")
                    .expiredAt(LocalDateTime.now().plusMinutes(15))
                    .usedAt(null)
                    .build();

            // We need to mock findByTokenHash with the hash of our raw token.
            // Since we can't predict the hash of an arbitrary string, we test via reflection.
            // Instead, let's test the service behaviour by constructing the right mock:
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            TokenVerificationResponse response = service.verifyToken("some-raw-token");
            assertThat(response.getStatus()).isEqualTo("valid");
        }

        @Test
        @DisplayName("should return 'expired' for an expired token")
        void expiredToken() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash("somehash")
                    .expiredAt(LocalDateTime.now().minusMinutes(5))
                    .usedAt(null)
                    .build();
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            TokenVerificationResponse response = service.verifyToken("some-raw-token");
            assertThat(response.getStatus()).isEqualTo("expired");
        }

        @Test
        @DisplayName("should return 'used' for an already-used token")
        void usedToken() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash("somehash")
                    .expiredAt(LocalDateTime.now().plusMinutes(15))
                    .usedAt(LocalDateTime.now().minusMinutes(2))
                    .build();
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            TokenVerificationResponse response = service.verifyToken("some-raw-token");
            assertThat(response.getStatus()).isEqualTo("used");
        }

        @Test
        @DisplayName("should return 'invalid' for a non-existent token")
        void invalidToken() {
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            TokenVerificationResponse response = service.verifyToken("garbage-token");
            assertThat(response.getStatus()).isEqualTo("invalid");
        }
    }

    // ---- resetPassword ----

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("should update user password and mark token as used")
        void happyPath() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash("somehash")
                    .expiredAt(LocalDateTime.now().plusMinutes(15))
                    .usedAt(null)
                    .user(testUser)
                    .build();
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NewPass123!")).thenReturn("encodedNewPassword");

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("some-raw-token")
                    .newPassword("NewPass123!")
                    .confirmPassword("NewPass123!")
                    .build();

            service.resetPassword(request);

            // Password updated
            verify(userService).save(testUser);
            assertThat(testUser.getPassword()).isEqualTo("encodedNewPassword");

            // Token marked as used
            verify(tokenRepository).save(token);
            assertThat(token.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw PASSWORD_MISMATCH when passwords don't match")
        void passwordMismatch() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("some-raw-token")
                    .newPassword("NewPass123!")
                    .confirmPassword("DifferentPass!")
                    .build();

            assertThatThrownBy(() -> service.resetPassword(request))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        }

        @Test
        @DisplayName("should throw RESET_TOKEN_INVALID for non-existent token")
        void invalidToken() {
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("garbage")
                    .newPassword("NewPass123!")
                    .confirmPassword("NewPass123!")
                    .build();

            assertThatThrownBy(() -> service.resetPassword(request))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESET_TOKEN_INVALID);
        }

        @Test
        @DisplayName("should throw RESET_TOKEN_EXPIRED for expired token")
        void expiredToken() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash("somehash")
                    .expiredAt(LocalDateTime.now().minusMinutes(5))
                    .usedAt(null)
                    .user(testUser)
                    .build();
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("some-token")
                    .newPassword("NewPass123!")
                    .confirmPassword("NewPass123!")
                    .build();

            assertThatThrownBy(() -> service.resetPassword(request))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("should throw RESET_TOKEN_USED for already-used token")
        void usedToken() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash("somehash")
                    .expiredAt(LocalDateTime.now().plusMinutes(15))
                    .usedAt(LocalDateTime.now().minusMinutes(2))
                    .user(testUser)
                    .build();
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("some-token")
                    .newPassword("NewPass123!")
                    .confirmPassword("NewPass123!")
                    .build();

            assertThatThrownBy(() -> service.resetPassword(request))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESET_TOKEN_USED);
        }
    }

    // ---- cleanupExpiredTokens ----

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class Cleanup {

        @Test
        @DisplayName("should delegate to repository and return count")
        void cleanup() {
            when(tokenRepository.deleteExpiredTokensBefore(any(LocalDateTime.class))).thenReturn(5);

            int deleted = service.cleanupExpiredTokens();

            assertThat(deleted).isEqualTo(5);
            verify(tokenRepository).deleteExpiredTokensBefore(any(LocalDateTime.class));
        }
    }
}
