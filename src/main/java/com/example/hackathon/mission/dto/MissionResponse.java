// src/main/java/com/example/hackathon/mission/dto/MissionResponse.java
package com.example.hackathon.mission.dto;

import com.example.hackathon.mission.entity.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MissionResponse {
    private Long missionId;
    private MissionCategory category;
    private PlaceCategory placeCategory;
    private String title;
    private String description;
    private VerificationType verificationType;
    private Integer minAmount;
    private Integer rewardPoint;
    private MissionStatus status;
    private LocalDate startDate;   // 시작일
    private LocalDate endDate;     // 종료일(+3주)
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // ✅ 공용 변환기: UserMission -> MissionResponse
    public static MissionResponse from(UserMission m) {
        return MissionResponse.builder()
                .missionId(m.getId())
                .category(m.getCategory())
                .placeCategory(m.getPlaceCategory())
                .title(m.getTitle())
                .description(m.getDescription())
                .verificationType(m.getVerificationType())
                .minAmount(m.getMinAmount())
                .rewardPoint(m.getRewardPoint())
                .status(m.getStatus())
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .createdAt(m.getCreatedAt())
                .startedAt(m.getStartedAt())
                .completedAt(m.getCompletedAt())
                .build();
    }

    // (옵션) 리스트 변환 헬퍼
    public static List<MissionResponse> fromList(List<UserMission> missions) {
        return missions.stream().map(MissionResponse::from).toList();
    }
}
