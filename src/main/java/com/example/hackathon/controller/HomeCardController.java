// src/main/java/com/example/hackathon/controller/HomeCardController.java
package com.example.hackathon.controller;

import com.example.hackathon.dto.home.HomeCardDTO;
import com.example.hackathon.dto.home.HomeCardResponseDTO;
import com.example.hackathon.entity.User;
import com.example.hackathon.mission.dto.MissionResponse;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.service.MissionService;
import com.example.hackathon.repository.UserRepository;
import com.example.hackathon.security.JwtTokenProvider;
import com.example.hackathon.service.HomeCardService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeCardController {

    private final HomeCardService homeCardService;
    private final MissionService missionService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    public ResponseEntity<HomeCardResponseDTO> getHome(HttpServletRequest request) {
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

        // 4) 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 5) 홈카드 정보
        HomeCardDTO homeCard = homeCardService.getCardByEmail(email);

        // 6) 랜덤 미션 3개 뽑기 → MissionResponse DTO로 변환
        List<MissionResponse> missions = missionService.getHomeMissions().stream()
                .map(MissionResponse::from)   // ✅ 공용 변환
                .toList();

        // 7) 최종 응답 DTO
        HomeCardResponseDTO response = HomeCardResponseDTO.builder()
                .homeCard(homeCard)
                .missions(missions)
                .build();

        return ResponseEntity.ok(response);
    }
}
