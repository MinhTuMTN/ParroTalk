package com.parrotalk.backend.dto.resend;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResendEmailResponse {
    private String id;
    private String name; // Often used for errors
    private String message; // Error message
}
