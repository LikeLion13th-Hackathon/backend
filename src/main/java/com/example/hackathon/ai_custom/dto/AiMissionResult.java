package com.example.hackathon.ai_custom.dto;

import com.example.hackathon.mission.entity.PlaceCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMissionResult {
    private Long missionId;
    private String category;
    private String title;
    private String description;

    public AiMissionResult(Long missionId, PlaceCategory category, String title, String description) {
        this.missionId = missionId;
        this.category = category.name(); 
        this.title = title;
        this.description = description;
    }
}
