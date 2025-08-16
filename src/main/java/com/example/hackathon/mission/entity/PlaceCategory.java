package com.example.hackathon.mission.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlaceCategory {
    CAFE("카페"),
    RESTAURANT("식당"),
    MUSEUM("박물관/미술관"),
    LIBRARY("도서관"),
    PARK("공원/산책로"),
    SPORTS_FACILITY("운동 시설"),
    SHOPPING_MALL("쇼핑센터"),
    TRADITIONAL_MARKET("전통 시장"),
    OTHER("기타");

    public final String label;

    PlaceCategory(String label) {
        this.label = label;
    }

    // JSON → Enum 변환 (한글/영어 둘 다 허용)
    @JsonCreator
    public static PlaceCategory from(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        for (PlaceCategory c : values()) {
            if (c.name().equalsIgnoreCase(trimmed)) return c;  // 영어(CAFE)
            if (c.label.equals(trimmed)) return c;             // 한글(카페)
        }
        throw new IllegalArgumentException("유효하지 않은 카테고리: " + value);
    }

    @JsonValue
    public String getLabel() {
        return this.label;
    }
}
