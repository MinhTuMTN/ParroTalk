package com.parrotalk.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Login request.
 * 
 * @author MinhTuMTN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** Email */
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    /** Password */
    @NotBlank(message = "Password is required")
    private String password;
}
