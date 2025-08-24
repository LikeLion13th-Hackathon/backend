// src/main/java/com/example/hackathon/dto/home/HomeCardDTO.java
package com.example.hackathon.dto.home;

import com.example.hackathon.dto.CharacterInfoDTO;
import lombok.*;



@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HomeCardDTO {
    private int coins;
    private String characterName;      // UI 표시용
    private CharacterInfoDTO character; // level, feedProgress, feedsRequiredToNext, activeBackgroundId
    private String backgroundName;     // 선택
}
