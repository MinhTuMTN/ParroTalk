package com.parrotalk.backend.constant;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    DUPLICATE_EMAIL(1001, "An account with this email already exists.", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1002, "Invalid credentials.", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED(1003, "Please verify your email before logging in.", HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN(1004, "Invalid refresh token.", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_INVALID(1005, "Verification token is invalid.", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_EXPIRED(1006, "Verification token has expired.", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_USED(1007, "Verification token has already been used.", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_SUPERSEDED(1008, "A newer verification email has already been sent.", HttpStatus.BAD_REQUEST),
    RESEND_COOLDOWN_ACTIVE(1009, "Please wait before requesting another verification email.", HttpStatus.TOO_MANY_REQUESTS),
    VALIDATION_ERROR(1010, "Validation failed.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(1500, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
