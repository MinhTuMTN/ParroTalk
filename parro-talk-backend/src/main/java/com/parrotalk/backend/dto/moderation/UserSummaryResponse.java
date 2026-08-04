package com.parrotalk.backend.dto.moderation;

import java.util.UUID;

import com.parrotalk.backend.entity.User;

/**
 * Minimal user projection exposed alongside reports and feedback.
 *
 * @param id       User id
 * @param fullName Display name
 * @param email    Contact email
 */
public record UserSummaryResponse(
        UUID id,
        String fullName,
        String email) {

    /**
     * Map a user entity, tolerating {@code null}.
     *
     * @param user User entity
     * @return Summary, or {@code null} when no user was given
     */
    public static UserSummaryResponse from(User user) {
        return user == null ? null : new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
