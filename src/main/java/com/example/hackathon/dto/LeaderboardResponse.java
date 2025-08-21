// src/main/java/com/example/hackathon/dto/LeaderboardResponse.java
package com.example.hackathon.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaderboardResponse {
    private Integer totalParticipants;   // 완료 경험 있는 사용자 수
    private List<LeaderboardEntry> top;  // Top N
}
