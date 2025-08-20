package com.example.hackathon.mission.controller;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.dto.MissionResponse;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.service.RegionMissionService;
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
@RequestMapping("/api/missions/region")
public class RegionMissionController {

    private final RegionMissionService regionMissionService;
    private final UserRepository userRepository;

    // ===== 공통 유틸 (MissionController와 동일 패턴) =====
    private String resolveEmail(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && StringUtils.hasText(auth.getName()) && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        String header = request.getHeader("X-USER-EMAIL"); // Postman용 우회
        return StringUtils.hasText(header) ? header : null;
    }

    private User currentUser(HttpServletRequest request) {
        String email = resolveEmail(request);
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("인증 정보가 없습니다. (JWT 또는 X-USER-EMAIL 필요)");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 유저가 없습니다: " + email));
    }

    private static MissionResponse toDto(UserMission m) {
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

    // 1) 지역 맛집 미션 조회
    @GetMapping("/restaurants")
    public ResponseEntity<?> listRestaurants(HttpServletRequest request) {
        User user = currentUser(request);
        List<UserMission> list = regionMissionService.listByCategory(user, MissionCategory.RESTAURANT);
        return ResponseEntity.ok(list.stream().map(RegionMissionController::toDto).toList());
    }

    // 2) 지역 맛집 미션 단건
    @GetMapping("/restaurants/{id}")
    public ResponseEntity<?> getRestaurant(HttpServletRequest request, @PathVariable Long id) {
        User user = currentUser(request);
        UserMission m = regionMissionService.getOneByCategory(user, id, MissionCategory.RESTAURANT);
        return ResponseEntity.ok(toDto(m));
    }

    // 3) 지역 명소 미션 조회
    @GetMapping("/landmarks")
    public ResponseEntity<?> listLandmarks(HttpServletRequest request) {
        User user = currentUser(request);
        List<UserMission> list = regionMissionService.listByCategory(user, MissionCategory.LANDMARK);
        return ResponseEntity.ok(list.stream().map(RegionMissionController::toDto).toList());
    }

    // 4) 지역 명소 미션 단건
    @GetMapping("/landmarks/{id}")
    public ResponseEntity<?> getLandmark(HttpServletRequest request, @PathVariable Long id) {
        User user = currentUser(request);
        UserMission m = regionMissionService.getOneByCategory(user, id, MissionCategory.LANDMARK);
        return ResponseEntity.ok(toDto(m));
    }

    // 5) 특산물 미션 조회
    @GetMapping("/specialties")
    public ResponseEntity<?> listSpecialties(HttpServletRequest request) {
        User user = currentUser(request);
        List<UserMission> list = regionMissionService.listByCategory(user, MissionCategory.SPECIALTY);
        return ResponseEntity.ok(list.stream().map(RegionMissionController::toDto).toList());
    }

    // 6) 특산물 미션 단건
    @GetMapping("/specialties/{id}")
    public ResponseEntity<?> getSpecialty(HttpServletRequest request, @PathVariable Long id) {
        User user = currentUser(request);
        UserMission m = regionMissionService.getOneByCategory(user, id, MissionCategory.SPECIALTY);
        return ResponseEntity.ok(toDto(m));
    }

}
