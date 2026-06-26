package com.parrotalk.backend.service;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.security.JwtUtils;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.dto.*;
import com.parrotalk.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.exception.AuthException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new AuthException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .displayUsername(buildDefaultUsername(request.getEmail()))
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // Default to normal USER
                .enabled(true)
                .emailVerified(false)
                .build();

        User savedUser = userService.save(user);
        emailVerificationService.issueVerificationEmail(savedUser);

        return AuthResponse.builder()
                .user(userMapper.toUserResponse(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User existingUser = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_CREDENTIALS));

        if (existingUser.getPassword() == null) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!existingUser.isEmailVerified()) {
            throw new AuthException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));
        } catch (AuthenticationException e) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = (User) authentication.getPrincipal();
        TokenPair tokenPair = tokenService.issueTokens(user);

        return AuthResponse.builder()
                .token(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    public AuthResponse refreshToken(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        String userEmail = jwtUtils.extractUsername(refreshToken);

        if (userEmail != null) {
            User user = userService.findByEmail(userEmail)
                    .orElseThrow(() -> new AuthException(ErrorCode.INVALID_REFRESH_TOKEN));

            if (jwtUtils.isTokenValid(refreshToken, user)) {
                String accessToken = jwtUtils.generateToken(user);
                return AuthResponse.builder()
                        .token(accessToken)
                        .refreshToken(refreshToken) // Re-use the same refresh token or rotate it
                        .user(userMapper.toUserResponse(user))
                        .build();
            }
        }
        throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    public UserResponse getCurrentUser(User user) {
        return userMapper.toUserResponse(user);
    }

    public String verifyEmail(VerifyEmailRequest request) {
        return emailVerificationService.verifyEmail(request.getToken());
    }

    public String resendVerificationEmail(ResendVerificationEmailRequest request) {
        return emailVerificationService.resendVerificationEmail(request.getEmail());
    }

    private String buildDefaultUsername(String email) {
        String localPart = email.substring(0, email.indexOf("@")).replaceAll("\\s+", "").toLowerCase();
        return localPart + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
