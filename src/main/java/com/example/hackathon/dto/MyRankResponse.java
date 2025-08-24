package com.example.hackathon.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MyRankResponse {
    private Integer myRank;               // 없으면 null
    private Integer myCompletedCount;     // 없으면 0
    private Integer totalParticipants;
    private List<LeaderboardEntry> around;   // 내 주변(±window)
}
