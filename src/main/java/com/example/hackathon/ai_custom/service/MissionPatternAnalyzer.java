package com.example.hackathon.ai_custom.service;

import com.example.hackathon.ai_custom.dto.AiMissionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionPatternAnalyzer {

    private final GeminiClient geminiClient;

    public List<String> analyzePattern(AiMissionRequest request) {
        String prompt = buildPrompt(request);
        return geminiClient.sendPrompt(prompt); // JSON → List<String> 카테고리 반환
    }

    private String buildPrompt(AiMissionRequest request) {
        return """
                당신은 사용자의 소비 패턴을 분석하는 AI입니다.
                입력: 사용자가 최근에 성공한 미션들의 장소 카테고리 리스트
                예: ["RESTAURANT", "RESTAURANT", "PARK"]

                규칙:
                1. 가장 많이 등장한 카테고리를 우선적으로 추천합니다.
                2. 총 3개의 카테고리를 반환해야 합니다.
                3. JSON 배열로 출력하세요. 예시:
                   ["RESTAURANT", "PARK", "CAFE"]

                최근 사용자 데이터: %s
                """.formatted(request.getCategories());
    }
}
