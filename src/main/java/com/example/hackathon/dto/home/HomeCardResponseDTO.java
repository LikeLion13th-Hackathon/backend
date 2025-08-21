// src/main/java/com/example/hackathon/dto/home/HomeCardResponseDTO.java
package com.example.hackathon.dto.home;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeCardResponseDTO {
    private HomeCardDTO homeCard;                       // 기존 캐릭터/코인/배경 정보
    private List<HomeCardMissionDTO> missions;          // 랜덤 미션 3개
}
