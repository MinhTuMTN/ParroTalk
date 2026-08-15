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
    private TokenService tokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        userMapper = mock(UserMapper.class);
        emailVerificationService = mock(EmailVerificationService.class);
        tokenService = mock(TokenService.class);
        authService = new AuthService(
                userService,
                passwordEncoder,
                mock(JwtUtils.class),
                authenticationManager,
                userMapper,
                tokenService,
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

        when(userService.findByEmailIncludeDeleted(request.getEmail())).thenReturn(Optional.empty());
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
    void registerThrowsDuplicateEmailWhenActiveEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("secret123")
                .fullName("User")
                .build();

        User existingActiveUser = User.builder()
                .email("existing@example.com")
                .emailVerified(true)
                .build();
        existingActiveUser.setDeleted(false);

        when(userService.findByEmailIncludeDeleted(request.getEmail())).thenReturn(Optional.of(existingActiveUser));

        AuthException exception = assertThrows(AuthException.class, () -> authService.register(request));
        org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.getErrorCode());
        verifyNoInteractions(passwordEncoder, emailVerificationService);
    }

    @Test
    void registerCreatesNewUserWhenDeletedEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("deleted@example.com")
                .password("newSecret")
                .fullName("New User")
                .build();

        User deletedUser = User.builder()
                .email("deleted@example.com")
                .fullName("Old User")
                .build();
        deletedUser.setDeleted(true);

        when(userService.findByEmailIncludeDeleted(request.getEmail())).thenReturn(Optional.of(deletedUser));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedNewSecret");
        when(userService.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userService.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(UserResponse.builder().email("deleted@example.com").build());

        authService.register(request);

        ArgumentCaptor<User> saveAndFlushCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveAndFlush(saveAndFlushCaptor.capture());
        
        ArgumentCaptor<User> saveCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(saveCaptor.capture());
        
        // The saveAndFlush is for updating the deleted user's email
        User updatedDeletedUser = saveAndFlushCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(updatedDeletedUser.getEmail().contains("_deleted_"));
        
        // The save is for the newly created user
        User newlyCreatedUser = saveCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("deleted@example.com", newlyCreatedUser.getEmail());
        org.junit.jupiter.api.Assertions.assertEquals("New User", newlyCreatedUser.getFullName());
        org.junit.jupiter.api.Assertions.assertEquals("encodedNewSecret", newlyCreatedUser.getPassword());
        org.junit.jupiter.api.Assertions.assertFalse(newlyCreatedUser.isEmailVerified());
        
        verify(emailVerificationService).issueVerificationEmail(newlyCreatedUser);
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

    @Test
    void loginUpdatesLastActiveAtOnSuccess() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password("encoded")
                .fullName("Active User")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build();

        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userService.updateLastActiveAt(user)).thenReturn(user);
        when(tokenService.issueTokens(user)).thenReturn(new com.parrotalk.backend.dto.TokenPair("access-token", "refresh-token"));
        when(userMapper.toUserResponse(user)).thenReturn(UserResponse.builder().email(user.getEmail()).build());

        authService.login(LoginRequest.builder()
                .email(user.getEmail())
                .password("secret123")
                .build());

        verify(userService).updateLastActiveAt(user);
    }
}
