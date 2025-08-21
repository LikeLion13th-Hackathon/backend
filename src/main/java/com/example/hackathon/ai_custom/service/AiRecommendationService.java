// src/main/java/com/example/hackathon/ai_custom/service/AiRecommendationService.java
package com.example.hackathon.ai_custom.service;

import com.example.hackathon.ai_custom.config.GeminiConfig;
import com.example.hackathon.ai_custom.prompt.AiPromptLibrary;
import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate rest = new RestTemplate();

    /**
     * LLM이 켜져 있으면 Gemini로 추천, 아니면 간단 휴리스틱으로 3개 반환
     */
    public List<PlaceCategory> recommendPlaces(User user, List<PlaceCategory> recentCompleted) {
        List<PlaceCategory> prefs = new ArrayList<>();
        if (user.getPref1()!=null) prefs.add(user.getPref1());
        if (user.getPref2()!=null) prefs.add(user.getPref2());
        if (user.getPref3()!=null) prefs.add(user.getPref3());

        if (geminiConfig.isEnabled()) {
            try {
                return callGemini(prefs, recentCompleted);
            } catch (Exception ignore) {
                // 실패하면 휴리스틱으로 폴백
            }
        }
        return heuristicTop3(prefs, recentCompleted);
    }

    // ---- Gemini 호출부 ----
    private List<PlaceCategory> callGemini(List<PlaceCategory> prefs, List<PlaceCategory> recent) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiConfig.getApiKey();

        String systemPrompt = AiPromptLibrary.SYSTEM_PROMPT;
        String userPrompt   = AiPromptLibrary.buildUserPrompt(prefs, recent);

        // Gemini 요청 JSON
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("role", "user",
                               "parts", List.of(Map.of("text", systemPrompt + "\n\n" + userPrompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = rest.postForEntity(url, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody()==null) {
            throw new IllegalStateException("Gemini error: " + resp.getStatusCode());
        }

        // 후보에서 첫 응답 텍스트 추출
        JsonNode root = objectMapper.readTree(resp.getBody());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) throw new IllegalStateException("No candidates");
        String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();

        // 텍스트가 {"places":["RESTAURANT","PARK","..."]} 형식이라고 가정
        JsonNode placesNode = objectMapper.readTree(text).path("places");
        if (!placesNode.isArray() || placesNode.size()<1) throw new IllegalStateException("Bad JSON from LLM");

        List<PlaceCategory> out = new ArrayList<>();
        for (JsonNode n: placesNode) {
            String v = n.asText();
            try {
                out.add(PlaceCategory.valueOf(v));
            } catch (IllegalArgumentException ignore) { /* skip invalid */ }
            if (out.size() == 3) break;
        }
        if (out.isEmpty()) throw new IllegalStateException("Empty places from LLM");
        while (out.size() < 3) { // 보정
            for (PlaceCategory pc : PlaceCategory.values()) {
                if (!out.contains(pc)) out.add(pc);
                if (out.size()==3) break;
            }
        }
        return out.subList(0, 3);
    }

    // ---- 간단 휴리스틱 폴백 ----
    private List<PlaceCategory> heuristicTop3(List<PlaceCategory> prefs, List<PlaceCategory> recent) {
        // 최근 완료 가중치 2, 선호 가중치 1로 랭킹
        Map<PlaceCategory, Integer> score = new EnumMap<>(PlaceCategory.class);
        for (PlaceCategory p : recent) score.merge(p, 2, Integer::sum);
        for (PlaceCategory p : prefs)  score.merge(p, 1, Integer::sum);

        List<PlaceCategory> all = new ArrayList<>(Arrays.asList(PlaceCategory.values()));
        all.sort((a,b) -> Integer.compare(score.getOrDefault(b,0), score.getOrDefault(a,0)));

        // 상위 3개
        List<PlaceCategory> out = new ArrayList<>();
        for (PlaceCategory pc : all) {
            if (pc == null) continue;
            out.add(pc);
            if (out.size()==3) break;
        }
        return out;
    }
}
