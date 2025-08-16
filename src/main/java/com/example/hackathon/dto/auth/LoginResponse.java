package com.example.hackathon.dto.auth;

public record LoginResponse(
        Integer userId,
        String nickname,
        String email,
        String accessToken,   // JWT 액세스 토큰
        String tokenType, 
        String message
) {}