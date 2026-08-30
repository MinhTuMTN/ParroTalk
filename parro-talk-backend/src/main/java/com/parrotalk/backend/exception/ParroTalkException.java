package com.parrotalk.backend.exception;

import org.springframework.http.HttpStatusCode;

import com.parrotalk.backend.constant.ErrorCode;

import lombok.Getter;

/**
 * Custom application exception.
 * 
 * @author MinhTuMTN
 */
@Getter
public class ParroTalkException extends RuntimeException {

    /** HTTP status code */
    private final HttpStatusCode httpStatusCode;

    /** Error code */
    private final String errorCode;

    public ParroTalkException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.name();
        this.httpStatusCode = errorCode.getHttpStatus();
    }

    public ParroTalkException(String message, HttpStatusCode httpStatusCode) {
        this(message, null, httpStatusCode);
    }

    public ParroTalkException(String message, String errorCode, HttpStatusCode httpStatusCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }
}
