package com.parrotalk.backend.exception;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public class ParroTalkException extends RuntimeException {
    private final HttpStatusCode httpStatusCode;

    public ParroTalkException(String message, HttpStatusCode httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }
}
