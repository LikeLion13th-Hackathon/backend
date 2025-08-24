package com.example.hackathon.mission.controller;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.dto.CompleteRequest;
import com.example.hackathon.mission.dto.MissionResponse;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.service.MissionService;
import com.example.hackathon.repository.UserRepository;
import com.example.hackathon.service.CoinService;
import com.example.hackathon.ai_custom.service.AiMissionService; // ★ AI 트리거용
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
    private final CoinService coinService;
    private final AiMissionService aiMissionService; // ★ 완료 시 트리거

    // ===================== 공통 유틸 =====================

    private String resolveEmail(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && StringUtils.hasText(auth.getName()) && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        // Postman 등 테스트용 헤더
        String header = request.getHeader("X-USER-EMAIL");
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

    // ===================== 기존 API =====================

    /** 전체/상태별 목록 */
    @GetMapping
    public ResponseEntity<?> listAllOrByStatus(
            HttpServletRequest request,
            @RequestParam(required = false) MissionStatus status
    ) {
        User user = currentUser(request);

        List<UserMission> list = (status == null)
                ? missionService.listAllMissions(user)
                : missionService.listMissionsByStatus(user, status);

        var res = list.stream().map(MissionController::toDto).toList();
        return ResponseEntity.ok(res);
    }

    /** 진행중만 */
    @GetMapping("/in-progress")
    public ResponseEntity<?> listInProgress(HttpServletRequest request) {
        User user = currentUser(request);
        var res = missionService.listMissionsByStatus(user, MissionStatus.IN_PROGRESS)
                .stream().map(MissionController::toDto).toList();
        return ResponseEntity.ok(res);
    }

    /** 완료만 */
    @GetMapping("/completed")
    public ResponseEntity<?> listCompleted(HttpServletRequest request) {
        User user = currentUser(request);
        var res = missionService.listMissionsByStatus(user, MissionStatus.COMPLETED)
                .stream().map(MissionController::toDto).toList();
        return ResponseEntity.ok(res);
    }

    /** 맞춤(사용자 선호 기반) 목록 */
    @GetMapping("/custom")
    public ResponseEntity<?> listCustomMissions(HttpServletRequest request) {
        User user = currentUser(request);

        List<PlaceCategory> prefs = List.of(
                user.getPref1(),
                user.getPref2(),
                user.getPref3()
        );

        // 최초 진입 시 기본 미션 보장
        missionService.ensureInitialMissions(user, prefs);

        List<UserMission> list = missionService.listCustomMissions(user);
        var res = list.stream().map(MissionController::toDto).toList();
        return ResponseEntity.ok(res);
    }

    /** 상세 조회 — 숫자만 매칭되도록 정규식 추가(비숫자 경로와 충돌 방지) */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<?> getMission(HttpServletRequest request, @PathVariable Long id){
        User user = currentUser(request);
        UserMission m = missionService.getUserMission(user, id);
        return ResponseEntity.ok(toDto(m));
    }

    /** 시작 */
    @PostMapping("/{id:\\d+}/start")
    public ResponseEntity<?> startMission(HttpServletRequest request, @PathVariable Long id){
        User user = currentUser(request);
        UserMission m = missionService.start(user, id);
        return ResponseEntity.ok(toDto(m));
    }

    /**
     * ✅ 완료
     * - MissionService.completeAuto(...) 안에서 @Transactional 로 상태가 DB에 반영된 뒤
     *   여기서 보상 지급 → AI 생성 트리거 순으로 호출한다.
     */
    @PostMapping("/{id:\\d+}/complete")
    public ResponseEntity<?> completeMission(HttpServletRequest request,
                                             @PathVariable Long id,
                                             @RequestBody(required = false) CompleteRequest body){
        User user = currentUser(request);
        Long receiptId = (body != null ? body.getReceiptId() : null);

        // 1) 완료 처리 (서비스 내부 트랜잭션에서 저장/flush/커밋)
        UserMission m = missionService.completeAuto(user, id, receiptId);

        // 2) 완료가 확정된 경우에만 보상 + AI 트리거
        if (m.getStatus() == MissionStatus.COMPLETED) {
            coinService.addCoins(user, m.getRewardPoint());
            aiMissionService.handleMissionSuccess(user.getId().longValue()); // ★ AI 생성 트리거
        }

        return ResponseEntity.ok(toDto(m));
    }

    /** 포기 */
    @PostMapping("/{id:\\d+}/abandon")
    public ResponseEntity<?> abandonMission(HttpServletRequest request, @PathVariable Long id){
        User user = currentUser(request);
        UserMission m = missionService.abandon(user, id);
        return ResponseEntity.ok(toDto(m));
    }

    // ===================== DTO 변환 =====================

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
