package com.example.hackathon.ai_custom.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GeminiConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    // ⚠️ objectMapper 빈은 여기서 만들지 마세요.
    // Spring Boot 기본 ObjectMapper 또는 JacksonTimeConfig의 @Bean이 주입됩니다.
}
