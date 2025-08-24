package com.example.hackathon.dto.mypage;

import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.VerificationType;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class UserMissionSummaryDTO {
    private Long missionId;                 // user_mission.id
    private MissionCategory category;       // 미션 카테고리
    private PlaceCategory placeCategory;    // 장소 카테고리
    private String title;                   // 제목
    private String description;             // 설명
    private VerificationType verificationType; // 인증 방식
    private Integer minAmount;              // 최소 금액
    private Integer rewardPoint;            // 보상 코인
    private MissionStatus status;           // 상태
    private LocalDate startDate;            // 시작일
    private LocalDate endDate;              // 종료일
}
