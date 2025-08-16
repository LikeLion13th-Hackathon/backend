// src/main/java/com/example/hackathon/config/SecurityConfig.java
package com.example.hackathon.config;

import com.example.hackathon.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 운영 모드: /shop/** 포함 모든 API 기본 보호.
 * - /api/auth/**, 정적/헬스 경로만 공개
 * - @EnableMethodSecurity 로 @PreAuthorize 사용 가능 (역할 제어)
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity   // 필수는 아니지만, 역할 기반 @PreAuthorize 쓰고 싶을 때 편리
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 정적/루트/헬스
                .requestMatchers("/", "/index.html", "/error", "/favicon.ico",
                                 "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                // 인증 공개
                .requestMatchers("/api/auth/**").permitAll()

                // 상점/그 외: 인증 필요
                .requestMatchers("/shop/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
