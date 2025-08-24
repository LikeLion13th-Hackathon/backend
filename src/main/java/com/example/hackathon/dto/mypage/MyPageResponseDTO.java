package com.example.hackathon.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 마이페이지 응답 DTO
 * - 내 정보(닉네임/이메일/생일/직업/지역/선호 장소)
 * - 미션 현황
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPageResponseDTO {

    // ===== 유저 기본 정보 =====
    private String nickname;
    private String email;
    private LocalDate birthDate;
    private String job; // STUDENT, EMPLOYEE 등

    // ===== 지역 =====
    private String regionSido;
    private String regionGungu;
    private String regionDong;

    // ===== 선호 장소 =====
    private Set<String> preferPlaces; // CAFE, LIBRARY, RESTAURANT 등
}
