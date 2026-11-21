package com.parrotalk.backend.exception;

import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.ratelimit.RateLimitResult;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    private final ErrorCode errorCode;
    private final RateLimitResult rateLimitResult;

    public AuthException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public AuthException(ErrorCode errorCode, RateLimitResult rateLimitResult) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.rateLimitResult = rateLimitResult;
    }
}
