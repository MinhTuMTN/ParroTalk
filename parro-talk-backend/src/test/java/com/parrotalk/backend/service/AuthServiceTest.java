package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.dto.LoginRequest;
import com.parrotalk.backend.dto.RegisterRequest;
import com.parrotalk.backend.dto.UserResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.AuthException;
import com.parrotalk.backend.mapper.UserMapper;
import com.parrotalk.backend.security.JwtUtils;

class AuthServiceTest {

    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private UserMapper userMapper;
    private EmailVerificationService emailVerificationService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        userMapper = mock(UserMapper.class);
        emailVerificationService = mock(EmailVerificationService.class);
        authService = new AuthService(
                userService,
                passwordEncoder,
                mock(JwtUtils.class),
                authenticationManager,
                userMapper,
                mock(TokenService.class),
                emailVerificationService);
    }

    @Test
    void registerCreatesUnverifiedUserAndQueuesVerificationEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@example.com")
                .password("secret123")
                .fullName("New User")
                .build();
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password("encoded")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(false)
                .build();

        when(userService.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userService.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserResponse(savedUser)).thenReturn(UserResponse.builder().email(savedUser.getEmail()).build());

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isEmailVerified());
        verify(emailVerificationService).issueVerificationEmail(savedUser);
    }

    @Test
    void loginRejectsPasswordUserUntilEmailIsVerified() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("pending@example.com")
                .password("encoded")
                .fullName("Pending User")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(false)
                .build();
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.login(LoginRequest.builder()
                        .email(user.getEmail())
                        .password("secret123")
                        .build()));

        org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, exception.getErrorCode());
        verifyNoInteractions(authenticationManager);
    }
}
