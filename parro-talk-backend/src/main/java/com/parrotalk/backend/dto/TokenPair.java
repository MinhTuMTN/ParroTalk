package com.parrotalk.backend.dto;

/**
 * Internal application tokens issued after successful authentication.
 */
public record TokenPair(String accessToken, String refreshToken) {
}
