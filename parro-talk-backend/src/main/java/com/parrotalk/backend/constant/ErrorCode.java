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
    RESET_TOKEN_INVALID(1011, "Invalid reset link.", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_EXPIRED(1012, "Reset link has expired.", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_USED(1013, "This reset link has already been used.", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1014, "Passwords do not match.", HttpStatus.BAD_REQUEST),
    RATE_LIMIT_EXCEEDED(1015, "Too many requests. Please try again later.", HttpStatus.TOO_MANY_REQUESTS),
    REGISTRATION_DISABLED(1016, "New user registration is currently disabled.", HttpStatus.FORBIDDEN),

    // AI & Classification
    LESSON_NOT_FOUND(2001, "Lesson not found.", HttpStatus.NOT_FOUND),
    LESSON_TITLE_MISSING(2002, "Lesson title is missing or blank.", HttpStatus.BAD_REQUEST),
    LESSON_TRANSCRIPT_EMPTY(2003, "Lesson transcript is empty or not available.", HttpStatus.BAD_REQUEST),
    ACTIVE_CATEGORIES_NOT_FOUND(2004, "No active allowed categories available for classification.", HttpStatus.BAD_REQUEST),

    // Lesson Comprehension Quiz
    QUIZ_GENERATION_COOLDOWN(2101, "Please wait before regenerating the quiz.", HttpStatus.TOO_MANY_REQUESTS),
    QUIZ_NOT_FOUND(2102, "Quiz or attempt not found.", HttpStatus.NOT_FOUND),
    QUIZ_ALREADY_COMPLETED(2103, "This quiz attempt has already been completed.", HttpStatus.BAD_REQUEST),
    QUIZ_QUESTION_ALREADY_ANSWERED(2104, "This question has already been answered.", HttpStatus.BAD_REQUEST),
    QUIZ_INCOMPLETE(2105, "Please answer all questions before completing the quiz.", HttpStatus.BAD_REQUEST),
    QUIZ_TRANSCRIPT_NOT_READY(2106, "Lesson transcript is not ready for quiz generation.", HttpStatus.BAD_REQUEST),

    INTERNAL_SERVER_ERROR(1500, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
