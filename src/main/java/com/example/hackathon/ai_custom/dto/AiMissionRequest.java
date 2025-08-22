package com.example.hackathon.ai_custom.dto;

import com.example.hackathon.mission.entity.PlaceCategory;
import lombok.Data;

import java.util.List;

@Data
public class AiMissionRequest {
    private List<String> categories;

    public AiMissionRequest(List<String> categories) {
        this.categories = categories;
    }

    // ✅ PlaceCategory enum 리스트도 받을 수 있게 추가
    public AiMissionRequest(List<PlaceCategory> placeCategories, boolean fromEnum) {
        this.categories = placeCategories.stream()
                .map(Enum::name) // "RESTAURANT", "PARK" 같은 문자열로 변환
                .toList();
    }
}
