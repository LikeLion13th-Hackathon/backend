package com.example.hackathon.dto.auth;

public record LoginResponse(
    Integer userId,
    String nickname,
    String email,
    String message
) {}
