package com.example.hackathon.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPageResponseDTO {

    // 유저 기본 정보
    private String nickname;
    private String email;
    private LocalDate birthDate;
    private String job; // 직업

    // 동의 정보
    private boolean isOver14;
    private boolean marketingConsent;
    private boolean serviceAgreed;
    private boolean privacyAgreed;
    private boolean locationConsent;

    // 미션 현황
    private List<String> ongoingMissions;   // 진행 중인 미션 제목 리스트
    private List<String> completedMissions; // 완료된 미션 제목 리스트
    private List<String> likedMissions;     // 찜한 미션 제목 리스트
}
