package com.example.hackathon.ai_custom.controller;

import com.example.hackathon.ai_custom.service.AiMissionOrchestrator;
import com.example.hackathon.entity.User;
import com.example.hackathon.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/missions/ai")
@RequiredArgsConstructor
public class AiMissionController {

    private final AiMissionOrchestrator aiMissionOrchestrator; // ✅ 새 오케스트레이터 사용
    private final UserRepository userRepository;

    // ===== 공통 유틸 =====
    private String resolveEmail(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && StringUtils.hasText(auth.getName()) && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        String header = request.getHeader("X-USER-EMAIL"); // Postman 등 테스트용
        return StringUtils.hasText(header) ? header : null;
    }

    private User currentUser(HttpServletRequest request){
        String email = resolveEmail(request);
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("인증 정보가 없습니다. (JWT 또는 X-USER-EMAIL 필요)");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 유저가 없습니다: " + email));
    }

    /**
     * [테스트/데모용] AI 추천 미션을 수동으로 생성
     * - 실제 운영 흐름은 영수증 OCR 인증 완료 3개마다 자동 생성됨
     */
    @PostMapping("/generate")
    public ResponseEntity<Void> generateAiMissions(HttpServletRequest request) {
        User user = currentUser(request);
        aiMissionOrchestrator.recommendNextSet(user.getId().longValue());
        return ResponseEntity.noContent().build(); // 204
    }
}
