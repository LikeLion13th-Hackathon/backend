package com.example.hackathon.dto.auth;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record SignUpRequest(
    @NotBlank @Size(max=32) String nickname,
    @Email @NotBlank @Size(max=254) String email,
    @NotBlank @Size(min=8, max=72) String password,  // BCrypt 입력 권장 길이
    @NotNull LocalDate birthDate,
    @NotNull Boolean isOver14,
    @Size(max=10) String region,
    @NotNull Boolean marketingConsent,
    @NotNull Boolean serviceAgreed,
    @NotNull Boolean privacyAgreed
) {}
