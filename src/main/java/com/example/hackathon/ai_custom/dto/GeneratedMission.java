package com.example.hackathon.ai_custom.dto;

import com.example.hackathon.mission.entity.PlaceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM이 만들어준 미션 1개를 표현하는 DTO.
 * - placeCategory: 미션이 속하는 장소 카테고리(분석 3개 + 신규 1개 중 하나)
 * - minAmount / rewardPoint 는 데모 기준 기본값 허용(없으면 0/기본 보상)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMission {
    private String title;                     //ex) "이번 주는 커피 대신 건강차 챌린지!"
    private String description;               //ex) "오후 루틴 바꿔보기: 허브티/유자차 구매 후 영수증 인증"
    private PlaceCategory placeCategory;      //ex) CAFE, RESTAURANT, 등
    private Integer minAmount;                // null이면 0으로 간주 
    private Integer rewardPoint;              // null이면 기본 보상
}
