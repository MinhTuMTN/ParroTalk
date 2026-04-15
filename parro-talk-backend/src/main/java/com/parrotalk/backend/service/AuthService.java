package com.parrotalk.backend.service;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.security.JwtUtils;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.dto.*;
import com.parrotalk.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    public AuthResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // Default to normal USER
                .enabled(true) // For now, auto-enable. Verification can be added later.
                .build();

        User savedUser = userService.save(user);

        return AuthResponse.builder()
                .user(userMapper.toUserResponse(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    public AuthResponse refreshToken(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        String userEmail = jwtUtils.extractUsername(refreshToken);

        if (userEmail != null) {
            User user = userService.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (jwtUtils.isTokenValid(refreshToken, user)) {
                String accessToken = jwtUtils.generateToken(user);
                return AuthResponse.builder()
                        .token(accessToken)
                        .refreshToken(refreshToken) // Re-use the same refresh token or rotate it
                        .user(userMapper.toUserResponse(user))
                        .build();
            }
        }
        throw new RuntimeException("Invalid refresh token");
    }

    public UserResponse getCurrentUser(User user) {
        return userMapper.toUserResponse(user);
    }
}
