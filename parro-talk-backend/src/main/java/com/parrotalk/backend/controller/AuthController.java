package com.parrotalk.backend.controller;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.AuthService;
import com.parrotalk.backend.service.ForgotPasswordRateLimiter;
import com.parrotalk.backend.service.PasswordResetService;
import com.parrotalk.backend.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .result(authService.register(request))
                .message("Registration successful. Please verify your email.")
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .result(authService.login(request))
                .message("Login successful")
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .result(authService.refreshToken(request))
                .message("Token refreshed successfully")
                .build());
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(authService.verifyEmail(request))
                .build());
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(authService.resendVerificationEmail(request))
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .result(authService.getCurrentUser(user))
                .build());
    }

    // ---- Forgot / Reset Password ----

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        // Rate-limit by both IP and email
        String clientIp = extractClientIp(httpRequest);
        forgotPasswordRateLimiter.checkRateLimit("ip:" + clientIp);
        forgotPasswordRateLimiter.checkRateLimit("email:" + request.getEmail().trim().toLowerCase());

        passwordResetService.requestPasswordReset(request.getEmail());

        // Always return 200 to prevent email enumeration
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("If the account exists, a password reset email has been sent.")
                .build());
    }

    @GetMapping("/reset-password/verify")
    public ResponseEntity<ApiResponse<TokenVerificationResponse>> verifyResetToken(
            @RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.<TokenVerificationResponse>builder()
                .result(passwordResetService.verifyToken(token))
                .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Password has been reset successfully.")
                .build());
    }

    /** Extracts the real client IP, respecting X-Forwarded-For if present. */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
