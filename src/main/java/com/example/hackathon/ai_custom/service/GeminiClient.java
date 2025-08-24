package com.example.hackathon.ai_custom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // application-(local).yml 값 주입
    @Value("${gemini.api-key}")
    private String geminiApiKey;

    // 모델은 설정으로 바꿀 수 있게(없으면 기본값)
    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    private String endpoint() {
        // ✅ API 키는 Bearer 헤더가 아니라 쿼리 파라미터로!
        return "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;
    }

    /**
     * 프롬프트를 Gemini API에 전달하고, 추천 카테고리 리스트를 반환
     */
    public List<String> sendPrompt(String prompt) {
        try {
            // JSON 이스케이프
            String safe = prompt.replace("\\", "\\\\").replace("\"", "\\\"");
            String body = """
                {
                  "contents": [
                    {
                      "parts": [
                        {"text": "%s"}
                      ]
                    }
                  ]
                }
                """.formatted(safe);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            String responseBody = response.getBody();

            var root = objectMapper.readTree(responseBody);
            var candidates = root.path("candidates");
            if (candidates.isMissingNode() || !candidates.isArray() || candidates.size() == 0) {
                throw new RuntimeException("Gemini 응답에 candidates가 없습니다: " + responseBody);
            }

            String text = candidates.get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // 우선 JSON 배열 시도 → 실패 시 콤마/개행 분리로 폴백
            try {
                return objectMapper.readValue(text, List.class);
            } catch (Exception ignore) {
                return Arrays.stream(text.split("[,\\n]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String detail = e.getResponseBodyAsString();
            throw new RuntimeException("Gemini API 호출 실패: " + e.getStatusCode() + " " + detail, e);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API 호출 실패", e);
        }
    }
}
