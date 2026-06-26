package com.parrotalk.backend.dto;

import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {
    @NotBlank(message = "Full name is required.")
    private String fullName;

    @NotBlank(message = "Username is required.")
    @Pattern(regexp = "^\\S+$", message = "Username must not contain spaces.")
    private String username;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    private String email;

    @NotNull(message = "Role is required.")
    private Role role;

    @NotNull(message = "Status is required.")
    private UserStatus status;
}
