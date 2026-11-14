package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.CacheNames;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.dto.ChangePasswordRequest;
import com.parrotalk.backend.dto.UpdateUserProfileRequest;
import com.parrotalk.backend.dto.UserResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.AuthException;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.UserMapperImpl;
import com.parrotalk.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache userCache;

    @Mock
    private Cache adminUserDetailCache;

    private UserService userService;
    private UUID userId;
    private User existingUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, new UserMapperImpl(), cacheManager);
        userId = UUID.randomUUID();
        existingUser = User.builder()
                .id(userId)
                .email("learner@parrotalk.fun")
                .displayUsername("old_name")
                .fullName("Old Full Name")
                .password("$2a$10$encodedOldHash")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build();

        lenient().when(cacheManager.getCache(CacheNames.USER_CACHE)).thenReturn(userCache);
        lenient().when(cacheManager.getCache(CacheNames.ADMIN_USER_DETAIL_CACHE)).thenReturn(adminUserDetailCache);
    }

    @Test
    @DisplayName("updateProfile updates fullName, displayUsername, avatarUrl without altering email and evicts both caches")
    void updateProfile_UpdatesFieldsSafelyAndEvictsBothCaches() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByDisplayUsernameIgnoreCase("new.learner_1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .fullName("  Alice Learner  ")
                .username("  New.Learner_1 ")
                .avatarUrl("https://cdn.parrotalk.fun/avatars/alice.png")
                .build();

        UserResponse response = userService.updateProfile(userId, request);

        assertEquals("learner@parrotalk.fun", existingUser.getEmail());
        assertEquals("Alice Learner", response.getFullName());
        assertEquals("new.learner_1", response.getUsername());
        assertEquals("https://cdn.parrotalk.fun/avatars/alice.png", response.getAvatarUrl());
        assertTrue(response.isHasPassword());

        verify(userCache).evict("learner@parrotalk.fun");
        verify(adminUserDetailCache).evict(userId);
    }

    @Test
    @DisplayName("updateProfile preserves avatarUrl when null, accepts relative path, and clears when empty or blank")
    void updateProfile_RelativeAndClearedAvatarUrl() {
        existingUser.setAvatarUrl("https://cdn.parrotalk.fun/avatars/keep.png");
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse preserved = userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                .fullName("Alice Keep Avatar")
                .avatarUrl(null)
                .build());
        assertEquals("https://cdn.parrotalk.fun/avatars/keep.png", preserved.getAvatarUrl());

        UserResponse withRelative = userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                .fullName("Alice")
                .avatarUrl("/uploads/avatar.png")
                .build());
        assertEquals("/uploads/avatar.png", withRelative.getAvatarUrl());

        UserResponse clearedEmpty = userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                .fullName("Alice")
                .avatarUrl("")
                .build());
        assertNull(clearedEmpty.getAvatarUrl());

        existingUser.setAvatarUrl("https://cdn.parrotalk.fun/avatars/reset.png");
        UserResponse clearedBlank = userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                .fullName("Alice")
                .avatarUrl("   ")
                .build());
        assertNull(clearedBlank.getAvatarUrl());
    }

    @Test
    @DisplayName("updateProfile rejects duplicate username owned by another user and unsafe avatar URLs")
    void updateProfile_RejectsDuplicateUsernameAndUnsafeAvatar() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        User otherUser = User.builder().id(UUID.randomUUID()).email("other@parrotalk.fun").build();
        when(userRepository.findByDisplayUsernameIgnoreCase("taken_name")).thenReturn(Optional.of(otherUser));

        ParroTalkException dupEx = assertThrows(ParroTalkException.class,
                () -> userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                        .fullName("Alice")
                        .username("taken_name")
                        .build()));
        assertEquals("DUPLICATE_USERNAME", dupEx.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, dupEx.getHttpStatusCode());

        ParroTalkException badUrlEx = assertThrows(ParroTalkException.class,
                () -> userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                        .fullName("Alice")
                        .avatarUrl("javascript:alert(1)")
                        .build()));
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), badUrlEx.getErrorCode());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfile preserves existing username when null and rejects empty or blank username with 400")
    void updateProfile_NullUsernamePreservedAndBlankOrEmptyRejected() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse preserved = userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                .fullName("Alice Null Username")
                .username(null)
                .build());
        assertEquals("old_name", preserved.getUsername());
        assertEquals("old_name", existingUser.getDisplayUsername());

        ParroTalkException emptyEx = assertThrows(ParroTalkException.class,
                () -> userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                        .fullName("Alice")
                        .username("")
                        .build()));
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), emptyEx.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, emptyEx.getHttpStatusCode());

        ParroTalkException blankEx = assertThrows(ParroTalkException.class,
                () -> userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                        .fullName("Alice")
                        .username("   ")
                        .build()));
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), blankEx.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, blankEx.getHttpStatusCode());

        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("changePassword verifies current password for password users and evicts both caches")
    void changePassword_PasswordUserSuccessAndWrongCurrentRejected() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("WrongPass1!", "$2a$10$encodedOldHash")).thenReturn(false);

        ParroTalkException wrongEx = assertThrows(ParroTalkException.class,
                () -> userService.changePassword(userId, ChangePasswordRequest.builder()
                        .currentPassword("WrongPass1!")
                        .newPassword("NewPass123!")
                        .confirmPassword("NewPass123!")
                        .build()));
        assertEquals(ErrorCode.INVALID_CREDENTIALS.name(), wrongEx.getErrorCode());

        when(passwordEncoder.matches("OldPass123!", "$2a$10$encodedOldHash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("$2a$10$encodedNewHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.changePassword(userId, ChangePasswordRequest.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewPass123!")
                .confirmPassword("NewPass123!")
                .build());

        assertEquals("$2a$10$encodedNewHash", existingUser.getPassword());
        verify(userCache).evict("learner@parrotalk.fun");
        verify(adminUserDetailCache).evict(userId);
    }

    @Test
    @DisplayName("changePassword allows OAuth passwordless user to set first local password without currentPassword")
    void changePassword_OAuthPasswordlessUserAllowedWithoutCurrentPassword() {
        existingUser.setPassword(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("FirstLocal123!")).thenReturn("$2a$10$firstHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.changePassword(userId, ChangePasswordRequest.builder()
                .currentPassword(null)
                .newPassword("FirstLocal123!")
                .confirmPassword("FirstLocal123!")
                .build());

        assertEquals("$2a$10$firstHash", existingUser.getPassword());
        verify(userCache).evict("learner@parrotalk.fun");
        verify(adminUserDetailCache).evict(userId);
    }

    @Test
    @DisplayName("changePassword throws PASSWORD_MISMATCH when newPassword and confirmPassword differ")
    void changePassword_MismatchThrowsAuthException() {
        AuthException ex = assertThrows(AuthException.class,
                () -> userService.changePassword(userId, ChangePasswordRequest.builder()
                        .currentPassword("OldPass123!")
                        .newPassword("NewPass123!")
                        .confirmPassword("Different123!")
                        .build()));
        assertEquals(ErrorCode.PASSWORD_MISMATCH, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cache eviction defers until TransactionSynchronization afterCommit when synchronization is active")
    void cacheEviction_DefersUntilAfterCommitInsideTransactionSynchronization() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("NewSyncPass1!")).thenReturn("$2a$10$syncHash");

        TransactionSynchronizationManager.initSynchronization();
        try {
            userService.updateProfile(userId, UpdateUserProfileRequest.builder()
                    .fullName("Sync Updated")
                    .build());

            verify(userCache, never()).evict(any());
            verify(adminUserDetailCache, never()).evict(any());

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            verify(userCache, times(1)).evict("learner@parrotalk.fun");
            verify(adminUserDetailCache, times(1)).evict(userId);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        TransactionSynchronizationManager.initSynchronization();
        try {
            existingUser.setPassword(null);
            userService.changePassword(userId, ChangePasswordRequest.builder()
                    .newPassword("NewSyncPass1!")
                    .confirmPassword("NewSyncPass1!")
                    .build());

            verify(userCache, times(1)).evict("learner@parrotalk.fun");
            verify(adminUserDetailCache, times(1)).evict(userId);

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            verify(userCache, times(2)).evict("learner@parrotalk.fun");
            verify(adminUserDetailCache, times(2)).evict(userId);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
