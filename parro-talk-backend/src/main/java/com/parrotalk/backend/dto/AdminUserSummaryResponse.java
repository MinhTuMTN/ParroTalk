package com.parrotalk.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Summary DTO for admin user listing endpoint.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserSummaryResponse {

    /** User ID **/
    private UUID id;

    /** Full name **/
    private String fullName;

    /** Display username **/
    private String username;

    /** Email address **/
    private String email;

    /** User role **/
    private Role role;

    /** Account status **/
    private UserStatus status;

    /** Avatar URL **/
    private String avatarUrl;

    /** Email verification status **/
    private boolean emailVerified;

    /** Account creation date **/
    private LocalDateTime createdAt;

    /** Last active timestamp **/
    private LocalDateTime lastActiveAt;
}
