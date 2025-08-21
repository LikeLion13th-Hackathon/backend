// src/main/java/com/example/hackathon/dto/home/HomeCardMissionDTO.java
package com.example.hackathon.dto.home;

import com.example.hackathon.mission.entity.MissionCategory;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeCardMissionDTO {
    private Long id;
    private MissionCategory category;
    private String title;
    private String description;
    private int rewardPoint;
}
