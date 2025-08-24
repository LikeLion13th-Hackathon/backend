package com.example.hackathon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            // allowedOrigins는 와일드카드 불가 → 배포/로컬만 정확히 기입
            .allowedOrigins(
                    "http://localhost:3000",
                    "https://missionpick.netlify.app"
            )
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
  }
}
