package com.parrotalk.backend.service;

import org.springframework.stereotype.Service;

import com.parrotalk.backend.dto.TokenPair;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.security.JwtUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtils jwtUtils;

    public TokenPair issueTokens(User user) {
        return new TokenPair(
                jwtUtils.generateToken(user),
                jwtUtils.generateRefreshToken(user));
    }
}
