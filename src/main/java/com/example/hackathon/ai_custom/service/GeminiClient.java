package com.example.hackathon.ai_custom.service;

import com.example.hackathon.ai_custom.dto.GeneratedMission;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Gemini 호출기
 * - PromptBuilder가 만든 프롬프트를 전송
 * - LLM이 돌려준 JSON 배열을 GeneratedMission 리스트로 파싱
 *
 * 응답 기대 포맷 (예시):
 * [
 *   {"title":"...", "description":"...", "category":"CAFE", "minAmount":0, "rewardPoint":120},
 *   ...
 * ]
 *  - category는 PlaceCategory enum 이름과 일치하도록 요청 (CAFE, RESTAURANT, ...)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    // 필요 시 최신 모델로 교체 가능
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    /**
     * 프롬프트를 전송하고 LLM이 생성한 미션 배열을 반환
     */
    public List<GeneratedMission> generateMissions(String prompt) {
        try {
            String bodyJson = buildRequestBody(prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            String url = GEMINI_API_URL + "?key=" + geminiApiKey;
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("[Gemini] non-2xx or empty body: status={}, body={}",
                        resp.getStatusCode(), shortBody(resp.getBody()));
                return List.of();
            }

            String text = extractTextFromGeminiResponse(resp.getBody());
            if (text == null || text.isBlank()) {
                log.warn("[Gemini] no text candidates found. raw={}", shortBody(resp.getBody()));
                return List.of();
            }

            // 코드블록 ```json ... ``` 로 감싸져 있을 수도 있으니 제거
            String cleaned = stripCodeFence(text);
            List<GeneratedMission> missions = parseMissionsJson(cleaned);

            log.info("[Gemini] parsed {} missions", missions.size());
            return missions;
        } catch (Exception e) {
            log.error("[Gemini] failed to call/parse", e);
            return List.of();
        }
    }

    /** Gemini v1beta generateContent 요청 바디 생성 */
    private String buildRequestBody(String prompt) {
        // 단순 텍스트 요청
        Map<String, Object> req = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        req.put("contents", List.of(content));
        try {
            return objectMapper.writeValueAsString(req);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Gemini 응답에서 text 추출 (가장 첫 candidate 우선) */
    private String extractTextFromGeminiResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode candidates = root.path("candidates");
            if (candidates.isMissingNode() || !candidates.isArray() || candidates.isEmpty()) return null;

            for (JsonNode c : candidates) {
                JsonNode content = c.path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray()) {
                    for (JsonNode p : parts) {
                        JsonNode textNode = p.get("text");
                        if (textNode != null && !textNode.asText("").isBlank()) {
                            return textNode.asText();
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("[Gemini] extract text failed", e);
            return null;
        }
    }

    /** ```json ... ``` 같은 코드펜스 제거 */
    private String stripCodeFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int idx = t.indexOf('\n');
            if (idx >= 0) t = t.substring(idx + 1).trim(); // 첫 줄 ```json 제거
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3).trim();
        }
        return t;
    }

    /** JSON 배열 → GeneratedMission 리스트 파싱(+유효성 보정) */
    private List<GeneratedMission> parseMissionsJson(String json) {
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(
                    json, new TypeReference<>() {});

            List<GeneratedMission> result = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                String title = str(m.get("title"));
                String description = str(m.get("description"));
                String cat = str(m.getOrDefault("category", m.get("placeCategory")));
                PlaceCategory place = safePlaceCategory(cat);

                Integer minAmount = toInt(m.get("minAmount"));
                Integer reward = toInt(m.get("rewardPoint"));

                if (title == null || title.isBlank()) continue;
                if (place == null) place = PlaceCategory.OTHER; // 기본 보정

                result.add(GeneratedMission.builder()
                        .title(title)
                        .description(description != null ? description : "")
                        .placeCategory(place)
                        .minAmount(minAmount != null ? minAmount : 0)
                        .rewardPoint(reward != null ? reward : 100)
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("[Gemini] JSON parse failed. json={}", shortBody(json), e);
            return List.of();
        }
    }

    private String str(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private PlaceCategory safePlaceCategory(String name) {
        if (name == null) return null;
        String key = name.trim().toUpperCase(Locale.ROOT);
        // 한글/별칭 들어올 때 간단 매핑
        switch (key) {
            case "CAFE", "CAFÉ", "카페" -> key = "CAFE";
            case "RESTAURANT", "식당", "음식점" -> key = "RESTAURANT";
            case "MUSEUM", "박물관", "미술관" -> key = "MUSEUM";
            case "LIBRARY", "도서관", "서점" -> key = "LIBRARY";
            case "PARK", "공원", "편의점" -> key = "PARK";
            case "SPORTS_FACILITY", "체육시설", "헬스장" -> key = "SPORTS_FACILITY";
            case "SHOPPING_MALL", "쇼핑몰", "마트" -> key = "SHOPPING_MALL";
            case "TRADITIONAL_MARKET", "전통시장", "시장" -> key = "TRADITIONAL_MARKET";
            default -> {
                // 그대로 enum 시도
            }
        }
        try {
            return PlaceCategory.valueOf(key);
        } catch (Exception e) {
            return PlaceCategory.OTHER;
        }
    }

    private String shortBody(String s) {
        if (s == null) return null;
        return s.length() > 600 ? s.substring(0, 600) + "...(trunc)" : s;
    }
}
