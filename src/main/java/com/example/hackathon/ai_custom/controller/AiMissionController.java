package com.example.hackathon.ai_custom.controller;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.dto.MissionResponse;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.repository.UserMissionRepository;
import com.example.hackathon.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/missions")
public class AiMissionController {

    private final UserMissionRepository repo;
    private final UserRepository userRepository;

    private String resolveEmail(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && StringUtils.hasText(auth.getName()) && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        String header = request.getHeader("X-USER-EMAIL");
        return StringUtils.hasText(header) ? header : null;
    }
    private User currentUser(HttpServletRequest request){
        String email = resolveEmail(request);
        if (!StringUtils.hasText(email)) throw new RuntimeException("인증 정보가 없습니다. (JWT 또는 X-USER-EMAIL 필요)");
        return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("해당 이메일의 유저가 없습니다: " + email));
    }
    private static MissionResponse toDto(UserMission m){
        return MissionResponse.builder()
                .missionId(m.getId())
                .category(m.getCategory())
                .placeCategory(m.getPlaceCategory())
                .title(m.getTitle())
                .description(m.getDescription())
                .verificationType(m.getVerificationType())
                .minAmount(m.getMinAmount())
                .rewardPoint(m.getRewardPoint())
                .status(m.getStatus())
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .createdAt(m.getCreatedAt())
                .startedAt(m.getStartedAt())
                .completedAt(m.getCompletedAt())
                .build();
    }

    // AI 맞춤 미션 목록
    @GetMapping("/ai")
    public ResponseEntity<?> listAiMissions(HttpServletRequest request) {
        User user = currentUser(request);
        List<UserMission> list = repo.findByUserAndCategoryOrderByCreatedAtAsc(user, MissionCategory.AI_CUSTOM);
        return ResponseEntity.ok(list.stream().map(AiMissionController::toDto).toList());
    }

    // AI 맞춤 미션 단건
    @GetMapping("/ai/{id}")
    public ResponseEntity<?> getAiMission(HttpServletRequest request, @PathVariable Long id) {
        User user = currentUser(request);
        UserMission m = repo.findByIdAndUser(id, user).orElseThrow(() -> new IllegalArgumentException("미션이 없거나 권한이 없습니다."));
        if (m.getCategory() != MissionCategory.AI_CUSTOM) throw new IllegalArgumentException("AI_CUSTOM 미션이 아닙니다.");
        return ResponseEntity.ok(toDto(m));
    }
}
