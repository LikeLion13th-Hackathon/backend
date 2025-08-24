package com.example.hackathon.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaderboardEntry {
    private Integer rank;            // 1,2,3...
    private Integer userId;
    private String  nickname;
    private Integer completedCount;  // 완료 미션 수
    private String  avatarUrl;       // 선택(없으면 null)
}
