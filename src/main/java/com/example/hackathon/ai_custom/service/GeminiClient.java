package com.example.hackathon.ai_custom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // application.yml 값 주입
    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    /**
     * 프롬프트를 Gemini API에 전달하고, 추천 카테고리 리스트를 반환
     */
    public List<String> sendPrompt(String prompt) {
        try {
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
            """.formatted(prompt.replace("\"", "\\\""));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(geminiApiKey); // yml에서 주입된 값 사용

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    GEMINI_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            String responseBody = response.getBody();

            // Gemini 응답에서 JSON 문자열 추출
            String jsonPart = objectMapper
                    .readTree(responseBody)
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // JSON 배열 → List<String>
            return objectMapper.readValue(jsonPart, List.class);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API 호출 실패", e);
        }
    }
}
