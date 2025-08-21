package com.example.hackathon.ai_custom.prompt;

import com.example.hackathon.mission.entity.PlaceCategory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gemini 프롬프트 유틸. 
 * - LLM에 "장소 카테고리 3개"만 JSON으로 반환하도록 강제.
 * - 후처리에서 템플릿 풀(MissionTemplate)에서 해당 카테고리별 미션을 뽑아 발급.
 */
public final class AiPromptLibrary {
    private AiPromptLibrary(){}

    public static final String SYSTEM_PROMPT = """
        당신은 소비 패턴을 분석해 '장소 카테고리 3개'를 추천하는 어시스턴트입니다.
        출력은 반드시 JSON으로만 하세요. 
        형식: {"places":["RESTAURANT","PARK","TRADITIONAL_MARKET"]}
        유효한 카테고리: CAFE, RESTAURANT, MUSEUM, LIBRARY, PARK, SPORTS_FACILITY, SHOPPING_MALL, TRADITIONAL_MARKET, OTHER
        규칙:
        - 최근에 자주 성공한 카테고리를 우선하되, 다양성도 고려하여 3개를 반환
        - 선호(preferences)가 최근 성공과 겹치면 더 높게 반영
        - 장소 문자열 외 불필요한 텍스트/설명은 절대 포함하지 말 것
        """;

    public static String buildUserPrompt(
            List<PlaceCategory> preferences,
            List<PlaceCategory> recentCompleted // 최근 N건 완료의 placeCategory 리스트
    ){
        String prefs = preferences.stream().map(Enum::name).collect(Collectors.joining(","));
        String recents = recentCompleted.stream().map(Enum::name).collect(Collectors.joining(","));

        return """
            선호카테고리: [%s]
            최근완료카테고리: [%s]
            다음 3주간 사용자 흥미와 소비 패턴에 맞는 장소 카테고리 3개를 JSON으로만 출력하세요.
            """.formatted(prefs, recents);
    }
}
