package com.example.hackathon.mission.dto;

import com.example.hackathon.mission.entity.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
