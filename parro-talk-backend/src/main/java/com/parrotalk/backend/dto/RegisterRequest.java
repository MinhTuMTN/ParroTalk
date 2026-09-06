package com.parrotalk.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 6)
    private String password;
    
    @NotBlank
    @Size(max = 50, message = "Full name must be less than 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Full name must contain only letters and spaces")
    private String fullName;
}
