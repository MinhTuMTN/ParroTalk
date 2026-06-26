package com.parrotalk.backend.controller;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.AuthService;
import com.parrotalk.backend.dto.*;
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
}
