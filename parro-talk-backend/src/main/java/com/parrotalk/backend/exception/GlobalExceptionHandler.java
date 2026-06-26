package com.parrotalk.backend.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(buildError(errorCode.getCode(), errorCode.name(), errorCode.getMessage()));
    }

    @ExceptionHandler(ParroTalkException.class)
    public ResponseEntity<ApiResponse<Void>> handleParroTalkException(ParroTalkException e) {
        return ResponseEntity
                .status(e.getHttpStatusCode())
                .body(buildError(
                        e.getHttpStatusCode().value(),
                        e.getErrorCode(),
                        e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "Invalid request" : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .badRequest()
                .body(buildError(
                        ErrorCode.VALIDATION_ERROR.getCode(),
                        ErrorCode.VALIDATION_ERROR.name(),
                        message.isBlank() ? ErrorCode.VALIDATION_ERROR.getMessage() : message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("Runtime error", e);
        return ResponseEntity
                .badRequest()
                .body(buildError(
                        HttpStatus.BAD_REQUEST.value(),
                        null,
                        e.getMessage() == null ? "Bad request" : e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleExceptions(Exception e) {
        log.error("Unhandled error", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    private ApiResponse<Void> buildError(int code, String errorCode, String message) {
        return ApiResponse.<Void>builder()
                .code(code)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
