package com.example.hackathon.mission.controller;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.dto.CompleteRequest;
import com.example.hackathon.mission.dto.MissionResponse;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.service.MissionService;
import com.example.hackathon.receipt.repository.ReceiptRepository;
import com.example.hackathon.repository.UserRepository;
import com.example.hackathon.service.CoinService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;
    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;
    private final CoinService coinService;

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

    private User currentUser(HttpServletRequest request) {
        String email = resolveEmail(request);
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("인증 정보가 없습니다. (JWT 또는 X-USER-EMAIL 필요)");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 유저가 없습니다: " + email));
    }

    // ===================== 목록/조회 =====================

    /**
     * 전체/상태별(+선택: 카테고리 필터) 목록
     * 사용 예:
     * - GET /api/missions
     * - GET /api/missions?status=COMPLETED
     * - GET /api/missions?category=AI_CUSTOM
     * - GET /api/missions?status=READY&category=AI_CUSTOM
     */
    @GetMapping
    public ResponseEntity<List<MissionResponse>> listAllOrByStatus(
            HttpServletRequest request,
            @RequestParam(required = false) MissionStatus status,
            @RequestParam(required = false) MissionCategory category) {
        User user = currentUser(request);

        List<UserMission> list = (status == null)
                ? missionService.listAllMissions(user)
                : missionService.listMissionsByStatus(user, status);

        if (category != null) {
            list = list.stream().filter(m -> m.getCategory() == category).toList();
        }

        List<MissionResponse> res = list.stream()
                .map(MissionResponse::from) // 공용 변환
                .toList();

        return ResponseEntity.ok(res);
    }

    /** 진행중만 (편의용) */
    @GetMapping("/in-progress")
    public ResponseEntity<List<MissionResponse>> listInProgress(HttpServletRequest request) {
        User user = currentUser(request);
        List<MissionResponse> res = missionService.listMissionsByStatus(user, MissionStatus.IN_PROGRESS)
                .stream().map(MissionResponse::from).toList(); // 공용 변환
        return ResponseEntity.ok(res);
    }

    /** 완료만 (편의용) */
    @GetMapping("/completed")
    public ResponseEntity<List<MissionResponse>> listCompleted(HttpServletRequest request) {
        User user = currentUser(request);
        List<MissionResponse> res = missionService.listMissionsByStatus(user, MissionStatus.COMPLETED)
                .stream().map(MissionResponse::from).toList(); // 공용 변환
        return ResponseEntity.ok(res);
    }

    // /** 맞춤(사용자 선호 기반) 목록 */
    // @GetMapping("/custom")
    // public ResponseEntity<List<MissionResponse>>
    // listCustomMissions(HttpServletRequest request) {
    // User user = currentUser(request);

    // List<PlaceCategory> prefs = List.of(
    // user.getPref1(),
    // user.getPref2(),
    // user.getPref3()
    // );

    // // 최초 진입 시 기본 미션 보장
    // missionService.ensureInitialMissions(user, prefs);

    // List<MissionResponse> res = missionService.listCustomMissions(user)
    // .stream().map(MissionResponse::from).toList(); // 공용 변환
    // return ResponseEntity.ok(res);
    // }
    /** 맞춤(사용자 선호 기반) 목록 */
@GetMapping("/custom")
public ResponseEntity<List<MissionResponse>> listCustomMissions(HttpServletRequest request) {
    User user = currentUser(request);

    // 현재 사용자 선호 장소(pref1~3)
    List<PlaceCategory> prefs = List.of(
            user.getPref1(),
            user.getPref2(),
            user.getPref3()
    );

    // 완전히 리프레시
    missionService.syncCustomMissions(user, prefs);

    // DB에서 방금 새로 생성된 미션만 가져오기
    List<MissionResponse> res = missionService.listCustomMissions(user)
            .stream().map(MissionResponse::from).toList();

    return ResponseEntity.ok(res);
}

// @GetMapping("/custom")
// public ResponseEntity<List<MissionResponse>> listCustomMissions(HttpServletRequest request) {
//     User user = currentUser(request);

//     // 현재 사용자 선호 장소(pref1~3)
//     List<PlaceCategory> prefs = List.of(
//             user.getPref1(),
//             user.getPref2(),
//             user.getPref3()
//     );

//     // 완전히 리프레시
//     missionService.syncCustomMissions(user, prefs);

//     // DB에서 새로 생성된 미션 중, 장소별로 1개씩만 선택
//     List<UserMission> allMissions = missionService.listCustomMissions(user);

//     // placeCategory별로 최신(createdAt 가장 늦은 것) 1개씩만 추출
//     Map<PlaceCategory, UserMission> latestByCategory = allMissions.stream()
//             .collect(Collectors.toMap(
//                     UserMission::getPlaceCategory,
//                     m -> m,
//                     (m1, m2) -> m1.getCreatedAt().isAfter(m2.getCreatedAt()) ? m1 : m2
//             ));

//     List<MissionResponse> res = latestByCategory.values().stream()
//             .map(MissionResponse::from)
//             .toList();

//     return ResponseEntity.ok(res);
// }



    /** 상세 조회 — 숫자만 매칭되도록 정규식 추가(비숫자 경로와 충돌 방지) */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<MissionResponse> getMission(HttpServletRequest request, @PathVariable Long id) {
        User user = currentUser(request);
        UserMission m = missionService.getUserMission(user, id);
        return ResponseEntity.ok(MissionResponse.from(m)); // 공용 변환
    }

    // ===================== 상태 변경 =====================

    /** 시작 */
    @PostMapping("/{id:\\d+}/start")
    public ResponseEntity<MissionResponse> startMission(HttpServletRequest request, @PathVariable Long id) {
        User user = currentUser(request);
        UserMission m = missionService.start(user, id);
        return ResponseEntity.ok(MissionResponse.from(m)); // 공용 변환
    }

    /**
     * 완료(자동 판단: PHOTO/RECEIPT_OCR)
     * - OCR 미션이면 body.receiptId 필요
     * - 서비스 내부 트랜잭션에서 저장 후, 완료되면 코인 지급
     */
    @PostMapping("/{id:\\d+}/complete")
    public ResponseEntity<MissionResponse> completeMission(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) CompleteRequest body) {
        User user = currentUser(request);
        Long receiptId = (body != null ? body.getReceiptId() : null);

        // 1) 완료 처리
        UserMission m = missionService.completeAuto(user, id, receiptId);

        // 2) 완료 확정 시 보상
        if (m.getStatus() == MissionStatus.COMPLETED) {
            coinService.addCoins(user, m.getRewardPoint());
        }

        return ResponseEntity.ok(MissionResponse.from(m)); // 공용 변환
    }

    /** 포기 */
    @PostMapping("/{id:\\d+}/abandon")
    public ResponseEntity<MissionResponse> abandonMission(HttpServletRequest request, @PathVariable Long id) {
        User user = currentUser(request);
        UserMission m = missionService.abandon(user, id);
        return ResponseEntity.ok(MissionResponse.from(m)); // 공용 변환
    }

    // ===================== 소비 통계 =====================

    /** (1) 월별 합계 + 미션 개수 */
    @GetMapping("/summary/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlySummary(HttpServletRequest request) {
        User user = currentUser(request);
        List<Object[]> rows = missionService.getMonthlySummary(user);

        var res = rows.stream().map(r -> Map.<String, Object>of(
                "month", r[0],
                "totalAmount", r[1],
                "missionsCompleted", r[2])).toList();

        return ResponseEntity.ok(res);
    }

    /** (2) 월별 + 카테고리별 소비 합계 */
    @GetMapping("/summary/monthly-category")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyCategorySummary(HttpServletRequest request) {
        User user = currentUser(request);
        List<Object[]> rows = missionService.getMonthlyCategorySummary(user);

        var res = rows.stream().map(r -> Map.<String, Object>of(
                "month", r[0],
                "placeCategory", r[1],
                "totalAmount", r[2],
                "missionsCompleted", r[3])).toList();

        return ResponseEntity.ok(res);
    }

    /** (3) 평균 소비 금액 */
    @GetMapping("/summary/average")
    public ResponseEntity<Map<String, Object>> getAverageSpending(HttpServletRequest request) {
        User user = currentUser(request);
        Double avg = missionService.getAverageSpending(user);
        return ResponseEntity.ok(Map.of("averageSpending", avg != null ? avg : 0.0));
    }
}
