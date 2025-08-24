package com.example.hackathon.dto.home;

import com.example.hackathon.mission.dto.MissionResponse;
import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HomeCardResponseDTO {
    private int coins;
    private int level;
    private String displayName;
    private double progressPercent;
    private Long activeCharacterId;
    private Long activeBackgroundId;
}

