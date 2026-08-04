package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerificationResponse {

    /**
     * One of: {@code "valid"}, {@code "expired"}, {@code "used"},
     * {@code "invalid"}.
     */
    private String status;
}
