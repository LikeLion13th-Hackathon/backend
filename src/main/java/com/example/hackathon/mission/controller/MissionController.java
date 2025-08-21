package com.example.hackathon.mission.controller;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.dto.CompleteRequest;
import com.example.hackathon.mission.dto.MissionResponse;
import com.example.hackathon.mission.entity.MissionCategory;   // ✅ 변경: CUSTOM 상수 사용을 위해 import
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.service.MissionService;
import com.example.hackathon.repository.UserRepository;
import com.example.hackathon.service.CoinService;   // ✅ 추가
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;                       // ✅ 변경: 정렬용
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;
    private final UserRepository userRepository;
    private final CoinService coinService;   // ✅ 추가

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

    // 맞춤 미션 목록 조회
    @GetMapping("/custom")
    public ResponseEntity<?> listCustomMissions(HttpServletRequest request) {
        User user = currentUser(request);

        List<PlaceCategory> prefs = List.of(
                user.getPref1(),
                user.getPref2(),
                user.getPref3()
        );

        missionService.ensureInitialMissions(user, prefs);

        // ✅ 변경: 혹시 레포지토리에서 섞여 들어오는 것을 방지하기 위해
        //         컨트롤러 레벨에서도 한 번 더 CUSTOM만 필터링 + 생성일 오름차순 정렬
        List<UserMission> list = missionService.listCustomMissions(user);
        var res = list.stream()
                .filter(m -> m.getCategory() == MissionCategory.CUSTOM)          // ✅ 변경
                .sorted(Comparator.comparing(UserMission::getCreatedAt))         // ✅ 변경(안전 정렬)
                .map(MissionController::toDto)
                .toList();

        return ResponseEntity.ok(res);
    }

    // 특정 미션 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getMission(HttpServletRequest request, @PathVariable Long id){
        User user = currentUser(request);
        UserMission m = missionService.getUserMission(user, id);
        return ResponseEntity.ok(toDto(m));
    }

    // 미션 시작 (상태: READY -> IN_PROGRESS)
    @PostMapping("/{id}/start")
    public ResponseEntity<?> startMission(HttpServletRequest request, @PathVariable Long id){
        User user = currentUser(request);
        UserMission m = missionService.start(user, id);
        return ResponseEntity.ok(toDto(m));
    }

    // 미션 완료 (PHOTO → 즉시 완료 / RECEIPT_OCR → receiptId 필요)
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeMission(HttpServletRequest request,
                                             @PathVariable Long id,
                                             @RequestBody(required = false) CompleteRequest body){
        User user = currentUser(request);
        Long receiptId = (body != null ? body.getReceiptId() : null);

        UserMission m = missionService.completeAuto(user, id, receiptId);

        // ✅ 미션이 COMPLETED 상태일 때만 코인 지급
        if (m.getStatus() == MissionStatus.COMPLETED) {
            coinService.addCoins(user, m.getRewardPoint());
        }

        return ResponseEntity.ok(toDto(m));
    }

    // 미션 포기 (상태: IN_PROGRESS -> ABANDONED)
    @PostMapping("/{id}/abandon")
    public ResponseEntity<?> abandonMission(HttpServletRequest request, @PathVariable Long id){
        User user = currentUser(request);
        UserMission m = missionService.abandon(user, id);
        return ResponseEntity.ok(toDto(m));
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
}
