// dto/home/HomeCardDTO.java
package com.example.hackathon.dto.home;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HomeCardDTO {
    private int coins;
    private String characterName;  // 없으면 "삐약이" 같은 고정값
    private int level;
    private double expPercent;     // 0~100
    private String backgroundName; // null 가능
}
