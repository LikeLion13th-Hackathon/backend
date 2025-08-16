package com.example.hackathon.dto.auth;

import com.example.hackathon.mission.entity.PlaceCategory;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record SignUpRequest(
    @NotBlank @Size(max=32)  String nickname,
    @Email    @NotBlank @Size(max=254) String email,
    @NotBlank @Size(min=8, max=72) String password,
    @NotNull  LocalDate birthDate,

    // ▼ 추가된 주소/역할/동의
    @NotBlank @Size(max=20)  String sido,
    @NotBlank @Size(max=30)  String sigungu,
    @NotBlank @Size(max=40)  String dong,
    @NotBlank @Size(max=20)  String role,
    @NotNull PlaceCategory pref1,
    @NotNull PlaceCategory pref2,
    @NotNull PlaceCategory pref3,
    @NotNull  Boolean locationConsent,

    @NotNull  Boolean marketingConsent,
    @NotNull  Boolean serviceAgreed,
    @NotNull  Boolean privacyAgreed
) {}

