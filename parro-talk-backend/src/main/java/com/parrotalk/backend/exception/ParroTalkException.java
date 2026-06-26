package com.parrotalk.backend.exception;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public class ParroTalkException extends RuntimeException {
    private final HttpStatusCode httpStatusCode;
    private final String errorCode;

    public ParroTalkException(String message, HttpStatusCode httpStatusCode) {
        this(message, null, httpStatusCode);
    }

    public ParroTalkException(String message, String errorCode, HttpStatusCode httpStatusCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }
}
