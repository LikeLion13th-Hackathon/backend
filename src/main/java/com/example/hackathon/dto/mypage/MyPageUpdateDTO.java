package com.example.hackathon.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 내 정보 변경 DTO (피그마 반영: 동의 항목 제외)
 * - email은 읽기 전용이므로 포함하지 않음
 * - birthDate는 "yyyy-MM-dd" 문자열로 전달(백엔드에서 파싱)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPageUpdateDTO {

    // 이름(닉네임)
    private String nickname;

    // 생년월일 (예: "2002-01-01")
    private String birthDate;

    // 직업: STUDENT, EMPLOYEE, FREELANCER, HOMEMAKER, ETC 등 프로젝트 enum 이름
    private String job;

    // 지역
    private String regionSido;   // 인천광역시
    private String regionGungu;  // 부평구
    private String regionDong;   // 부평동

    // 선호 장소: CAFE, LIBRARY, RESTAURANT, PARK, MUSEUM, GYM, SHOPPING, TRADITIONAL_MARKET, ETC ...
    private Set<String> preferPlaces;

    // 프로필 이미지 URL (선택)
    private String profileImageUrl;
}
