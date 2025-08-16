// src/main/java/com/example/hackathon/controller/HomeCardController.java
package com.example.hackathon.controller;

import com.example.hackathon.dto.home.HomeCardDTO;
import com.example.hackathon.security.JwtTokenProvider;
import com.example.hackathon.service.HomeCardService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeCardController {

    private final HomeCardService homeCardService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/card")
    public ResponseEntity<HomeCardDTO> getHomeCard(HttpServletRequest request) {
        // 1) Authorization 헤더에서 Bearer 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String token = authHeader.substring(7);

        // 2) 토큰 검증
        if (!jwtTokenProvider.validate(token)) {
            return ResponseEntity.status(401).build();
        }

        // 3) 클레임 파싱 → email(subject)
        Claims claims = jwtTokenProvider.parseClaims(token);
        String email = claims.getSubject();

        // 4) 서비스 호출
        HomeCardDTO dto = homeCardService.getCardByEmail(email);

        return ResponseEntity.ok(dto);
    }
}
