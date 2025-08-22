package com.example.hackathon.mission.controller;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.dto.CompleteRequest;
import com.example.hackathon.mission.dto.MissionResponse;
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

    // ===================== ✅ 추가: 전체/상태별 목록 조회 =====================

    /**
     * 전체 미션 목록 조회 (상태 파라미터 없으면 전체, 있으면 해당 상태만)
     * 예)
     *  - GET /api/missions                  -> 전체
     *  - GET /api/missions?status=IN_PROGRESS -> 진행중만
     *  - GET /api/missions?status=COMPLETED   -> 완료만
     */
    @GetMapping
    public ResponseEntity<?> listAllOrByStatus(
            HttpServletRequest request,
            @RequestParam(required = false) MissionStatus status
    ) {
        User user = currentUser(request);

        List<UserMission> list = (status == null)
                ? missionService.listAllMissions(user)               // 레포 수정 없이 카테고리 합쳐서 조회
                : missionService.listMissionsByStatus(user, status); // 메모리 필터

        var res = list.stream().map(MissionController::toDto).toList();
        return ResponseEntity.ok(res);
    }

    /** 진행중만 별칭 라우트 (프론트 편의용) */
    @GetMapping("/in-progress")
    public ResponseEntity<?> listInProgress(HttpServletRequest request) {
        User user = currentUser(request);
        var res = missionService.listMissionsByStatus(user, MissionStatus.IN_PROGRESS)
                .stream().map(MissionController::toDto).toList();
        return ResponseEntity.ok(res);
    }

    /** 완료만 별칭 라우트 (프론트 편의용) */
    @GetMapping("/completed")
    public ResponseEntity<?> listCompleted(HttpServletRequest request) {
        User user = currentUser(request);
        var res = missionService.listMissionsByStatus(user, MissionStatus.COMPLETED)
                .stream().map(MissionController::toDto).toList();
        return ResponseEntity.ok(res);
    }

    // ======================================================================

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

        List<UserMission> list = missionService.listCustomMissions(user);
        var res = list.stream().map(MissionController::toDto).toList();
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
