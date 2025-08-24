package com.example.hackathon.ai_custom.controller;

import com.example.hackathon.ai_custom.dto.AiMissionResult;
import com.example.hackathon.ai_custom.service.AiMissionService;
import com.example.hackathon.entity.User;
import com.example.hackathon.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missions/ai")
@RequiredArgsConstructor
public class AiMissionController {

    private final AiMissionService aiMissionService;
    private final UserRepository userRepository;

    // ===== 공통 유틸 =====
    private String resolveEmail(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && StringUtils.hasText(auth.getName()) && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        String header = request.getHeader("X-USER-EMAIL"); // Postman 테스트용
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

    /** 사용자 맞춤 미션 전체 조회 (최대 3개) */
    @GetMapping
    public List<AiMissionResult> getUserAiMissions(HttpServletRequest request) {
        User user = currentUser(request);
        return aiMissionService.getUserAiMissions(user.getId().longValue()); // ✅ Integer -> Long
    }

    /** 맞춤 미션 단건 조회 (숫자만 매칭되도록 정규식) */
    @GetMapping("/{missionId:\\d+}")
    public AiMissionResult getUserAiMissionById(HttpServletRequest request,
                                                @PathVariable Long missionId) {
        User user = currentUser(request);
        return aiMissionService.getUserAiMissionById(user.getId().longValue(), missionId); // ✅ Integer -> Long
    }

    @PostMapping("/generate")
    public void generateAiMissions(HttpServletRequest request) {
        User user = currentUser(request);                 // X-USER-EMAIL 헤더 or 인증에서 이메일 꺼냄
        aiMissionService.handleMissionSuccess(user.getId().longValue());
    }
}
