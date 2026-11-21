package com.parrotalk.backend.controller;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.ratelimit.ClientIpResolver;
import com.parrotalk.backend.ratelimit.RateLimit;
import com.parrotalk.backend.ratelimit.RateLimitKeyStrategy;
import com.parrotalk.backend.ratelimit.RateLimitTier;
import com.parrotalk.backend.service.AuthService;
import com.parrotalk.backend.service.ForgotPasswordRateLimiter;
import com.parrotalk.backend.service.PasswordResetService;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.AuthResponse;
import com.parrotalk.backend.dto.ForgotPasswordRequest;
import com.parrotalk.backend.dto.LoginRequest;
import com.parrotalk.backend.dto.RefreshRequest;
import com.parrotalk.backend.dto.RegisterRequest;
import com.parrotalk.backend.dto.ResendVerificationEmailRequest;
import com.parrotalk.backend.dto.ResetPasswordRequest;
import com.parrotalk.backend.dto.TokenVerificationResponse;
import com.parrotalk.backend.dto.UserResponse;
import com.parrotalk.backend.dto.VerifyEmailRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/register")
    @RateLimit(tier = RateLimitTier.AUTH, keyStrategy = RateLimitKeyStrategy.IP_ONLY)
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .result(authService.register(request))
                .message("Registration successful. Please verify your email.")
                .build());
    }

    @PostMapping("/login")
    @RateLimit(tier = RateLimitTier.AUTH, keyStrategy = RateLimitKeyStrategy.IP_ONLY)
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .result(authService.login(request))
                .message("Login successful")
                .build());
    }

    @PostMapping("/refresh")
    @RateLimit(tier = RateLimitTier.AUTH, keyStrategy = RateLimitKeyStrategy.IP_ONLY, resource = "refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .result(authService.refreshToken(request))
                .message("Token refreshed successfully")
                .build());
    }

    @PostMapping("/verify-email")
    @RateLimit(tier = RateLimitTier.AUTH, keyStrategy = RateLimitKeyStrategy.IP_ONLY)
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(authService.verifyEmail(request))
                .build());
    }

    @PostMapping("/resend-verification-email")
    @RateLimit(tier = RateLimitTier.AUTH_RESEND_VERIFICATION, keyStrategy = RateLimitKeyStrategy.IP_ONLY)
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
        String clientIp = clientIpResolver.resolve(httpRequest);
        forgotPasswordRateLimiter.checkRateLimit("ip:" + clientIp);
        forgotPasswordRateLimiter.checkRateLimit("email:" + request.getEmail().trim().toLowerCase());

        passwordResetService.requestPasswordReset(request.getEmail());

        // Always return 200 to prevent email enumeration
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("If the account exists, a password reset email has been sent.")
                .build());
    }

    @GetMapping("/reset-password/verify")
    @RateLimit(tier = RateLimitTier.AUTH, keyStrategy = RateLimitKeyStrategy.IP_ONLY)
    public ResponseEntity<ApiResponse<TokenVerificationResponse>> verifyResetToken(
            @RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.<TokenVerificationResponse>builder()
                .result(passwordResetService.verifyToken(token))
                .build());
    }

    @PostMapping("/reset-password")
    @RateLimit(tier = RateLimitTier.AUTH, keyStrategy = RateLimitKeyStrategy.IP_ONLY)
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Password has been reset successfully.")
                .build());
    }
}
