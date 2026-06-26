package com.parrotalk.backend.dto;

public record VerifyEmailMessage(
        String email,
        String fullName,
        String verificationUrl,
        long expirationMinutes,
        int attempt) {

    public VerifyEmailMessage nextAttempt() {
        return new VerifyEmailMessage(email, fullName, verificationUrl, expirationMinutes, attempt + 1);
    }
}
