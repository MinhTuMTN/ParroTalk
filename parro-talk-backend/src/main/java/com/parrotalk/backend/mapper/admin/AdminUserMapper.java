package com.parrotalk.backend.mapper.admin;

import java.time.LocalDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.parrotalk.backend.constant.UserStatus;
import com.parrotalk.backend.dto.AdminUserResponse;
import com.parrotalk.backend.dto.AdminUserSummaryResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserProgress;
import com.parrotalk.backend.entity.UserStreak;

/**
 * Mapper for Admin User DTO conversions.
 *
 * @author MinhTuMTN
 */
@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    /**
     * Maps User entity to AdminUserSummaryResponse.
     *
     * @param user User entity
     * @return AdminUserSummaryResponse DTO
     */
    @Mapping(target = "username", expression = "java(resolveUsername(user))")
    @Mapping(target = "status", expression = "java(resolveStatus(user))")
    @Mapping(target = "lastActiveAt", source = "lastActiveAt")
    AdminUserSummaryResponse toSummaryResponse(User user);

    /**
     * Maps User, UserProgress, and UserStreak entities to AdminUserResponse detail DTO.
     *
     * @param user User entity
     * @param progress UserProgress entity
     * @param streak UserStreak entity
     * @return AdminUserResponse detail DTO
     */
    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "username", expression = "java(resolveUsername(user))")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "status", expression = "java(resolveStatus(user))")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "emailVerified", source = "user.emailVerified")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "lastActiveAt", expression = "java(resolveLastActiveAt(user, progress))")
    @Mapping(target = "totalLessonsCompleted", source = "progress.totalLessonsCompleted", defaultValue = "0")
    @Mapping(target = "totalScore", source = "progress.totalScore", defaultValue = "0.0")
    @Mapping(target = "avgScore", source = "progress.avgScore", defaultValue = "0.0")
    @Mapping(target = "currentStreak", source = "streak.currentStreak", defaultValue = "0")
    @Mapping(target = "longestStreak", source = "streak.longestStreak", defaultValue = "0")
    AdminUserResponse toDetailResponse(User user, UserProgress progress, UserStreak streak);

    /**
     * Resolves display username from User entity.
     *
     * @param user User entity
     * @return Display username or email prefix
     */
    default String resolveUsername(User user) {
        if (user.getDisplayUsername() != null && !user.getDisplayUsername().isBlank()) {
            return user.getDisplayUsername();
        }

        return user.getEmail().substring(0, user.getEmail().indexOf("@"));
    }

    /**
     * Resolves user status based on enabled flag.
     *
     * @param user User entity
     * @return UserStatus
     */
    default UserStatus resolveStatus(User user) {
        return user.isEnabled() ? UserStatus.ACTIVE : UserStatus.INACTIVE;
    }

    /**
     * Resolves last active timestamp from User entity, falling back to progress last activity date.
     *
     * @param user User entity
     * @param progress UserProgress entity
     * @return LocalDateTime last active date
     */
    default LocalDateTime resolveLastActiveAt(User user, UserProgress progress) {
        if (user != null && user.getLastActiveAt() != null) {
            return user.getLastActiveAt();
        }
        return progress != null ? progress.getLastActivityDate() : null;
    }
}
