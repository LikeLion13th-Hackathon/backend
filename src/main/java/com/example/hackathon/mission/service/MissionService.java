package com.example.hackathon.mission.service;

import com.example.hackathon.ai_custom.service.AiMissionOrchestrator;
import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.*;
import com.example.hackathon.mission.repository.UserMissionRepository;
import com.example.hackathon.receipt.OcrStatus;
import com.example.hackathon.receipt.VerificationStatus;
import com.example.hackathon.receipt.entity.Receipt;
import com.example.hackathon.receipt.repository.ReceiptRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MissionService {

    private final UserMissionRepository repo;
    private final ReceiptRepository receiptRepository;

    // ★ 추가: AI 오케스트레이터 주입
    private final AiMissionOrchestrator aiMissionOrchestrator;

    // 데모용: 기간 체크 X
    @Value("${app.mission.verify.period:false}")
    private boolean checkPeriod;

    public MissionService(UserMissionRepository repo,
            ReceiptRepository receiptRepository,
            AiMissionOrchestrator aiMissionOrchestrator) { // 생성자에 주입
        this.repo = repo;
        this.receiptRepository = receiptRepository;
        this.aiMissionOrchestrator = aiMissionOrchestrator;
    }

    // ===================== 홈화면용 랜덤 미션 =====================
    public List<UserMission> getHomeMissions() {
        List<UserMission> result = new ArrayList<>();

        // 1) CUSTOM, AI_CUSTOM 중에서 랜덤으로 2개 선택
        List<UserMission> customMissions = repo.findByCategoryIn(
                List.of(MissionCategory.CUSTOM, MissionCategory.AI_CUSTOM));
        Collections.shuffle(customMissions);
        result.addAll(customMissions.stream().limit(2).toList());

        // 2) RESTAURANT, LANDMARK, SPECIALTY 중에서 랜덤으로 1개 선택
        List<UserMission> regionMissions = repo.findByCategoryIn(
                List.of(MissionCategory.RESTAURANT, MissionCategory.LANDMARK, MissionCategory.SPECIALTY));
        Collections.shuffle(regionMissions);
        regionMissions.stream().findFirst().ifPresent(result::add);

        return result;
    }
    // ============================================================

    // ====== 기본 템플릿 (1번 미션) ======
    private static final Map<PlaceCategory, Template> TPL = new EnumMap<>(PlaceCategory.class);
    static {
        put(PlaceCategory.CAFE, "카페에서 시원한 아아 한 잔 %s원 이상 인증하기", 3000, 80);
        put(PlaceCategory.RESTAURANT, "맛집에서 든든하게 %s원 이상 먹고 영수증 인증!", 7000, 100);
        put(PlaceCategory.MUSEUM, "박물관 티켓 영수증으로 지식 충전 인증하기", 3000, 90);
        put(PlaceCategory.LIBRARY, "책 냄새 맡으러 서점/문구점 %s원 이상 구매 인증", 2000, 100);
        put(PlaceCategory.PARK, "편의점에서 공원 간식 %s원 이상 사 먹기 인증", 2000, 90);
        put(PlaceCategory.SPORTS_FACILITY, "헬창 모드! 운동 시설 %s원 이상 결제 인증", 10000, 80);
        put(PlaceCategory.SHOPPING_MALL, "쇼핑센터에서 충동구매 %s원 이상 인증하기", 8000, 100);
        put(PlaceCategory.TRADITIONAL_MARKET, "시장 통닭/군밤 %s원 이상 사 먹고 인증!", 5000, 90);
        put(PlaceCategory.OTHER, "동네 가게에서 %s원 이상 소소한 소비 인증", 3000, 80);
    }

    private static void put(PlaceCategory c, String p, int min, int r) {
        TPL.put(c, new Template(p, min, r));
    }

    private record Template(String pattern, int minAmount, int rewardPoint) {
    }

    // ====== 보조 템플릿 (2번 미션: 금액/리워드/문구를 직접 지정) ======
    private static final Map<PlaceCategory, Template> TPL_ALT = new EnumMap<>(PlaceCategory.class);
    static {
        TPL_ALT.put(PlaceCategory.CAFE, new Template("카페에서 디저트까지 곁들여 %s원 이상 인증하기", 2000, 100));
        TPL_ALT.put(PlaceCategory.RESTAURANT, new Template("음식점에서 밥+후식까지 %s원 이상 영수증 인증", 5000, 80));
        TPL_ALT.put(PlaceCategory.SHOPPING_MALL, new Template("쇼핑몰에서 지갑 털고 %s원 이상 영수증 인증", 6000, 90));
        TPL_ALT.put(PlaceCategory.TRADITIONAL_MARKET, new Template("시장 떡볶이+튀김 %s원 이상 먹고 인증!", 3000, 100));
        TPL_ALT.put(PlaceCategory.OTHER, new Template("랜덤 가게에서 기분 소비 %s원 이상 영수증 인증", 2000, 80));

        TPL_ALT.put(PlaceCategory.MUSEUM, new Template("미술관 티켓 영수증으로 감성 충전 인증", 2000, 90));
        TPL_ALT.put(PlaceCategory.LIBRARY, new Template("도서관 복사/출력 %s원 이상 영수증 인증", 2000, 100));
        TPL_ALT.put(PlaceCategory.PARK, new Template("공원 자전거 대여 %s원 이상 영수증 인증", 2000, 100));
        TPL_ALT.put(PlaceCategory.SPORTS_FACILITY, new Template("헬스장 PT권 %s원 이상 결제 영수증 인증", 10000, 80));
    }

    // 회원가입 직후 초기 맞춤 미션 생성
    public void ensureInitialMissions(User user, List<PlaceCategory> prefs) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusWeeks(3);

        for (PlaceCategory p : prefs) {
            if (p == null)
                continue;

            List<UserMission> existing = repo.findByUserAndCategoryAndPlaceCategoryOrderByCreatedAtAsc(
                    user, MissionCategory.CUSTOM, p);

            Set<String> existingTitles = new HashSet<>();
            for (UserMission um : existing)
                existingTitles.add(um.getTitle());

            if (existing.size() < 2) {
                Template t1 = TPL.getOrDefault(p, TPL.get(PlaceCategory.OTHER));
                Template t2 = TPL_ALT.getOrDefault(p, TPL_ALT.get(PlaceCategory.OTHER));

                String prefix = (user.getDong() != null && !user.getDong().isBlank()) ? (user.getDong() + " ") : "";

                String title1 = buildTitle(prefix, t1.pattern(), t1.minAmount());
                if (!existingTitles.contains(title1)) {
                    repo.save(buildMission(user, p, title1, t1, start, end));
                    existingTitles.add(title1);
                    existing.add(new UserMission());
                }

                if (existing.size() < 2) {
                    String title2 = buildTitle(prefix, t2.pattern(), t2.minAmount());
                    if (!existingTitles.contains(title2)) {
                        repo.save(buildMission(user, p, title2, t2, start, end));
                        existingTitles.add(title2);
                        existing.add(new UserMission());
                    }
                }
            }
        }
    }

    private String buildTitle(String prefix, String pattern, int minAmount) {
        return pattern.contains("%s") ? (prefix + pattern.formatted(minAmount)) : (prefix + pattern);
    }

    private UserMission buildMission(User user, PlaceCategory p, String title, Template t,
            LocalDate start, LocalDate end) {
        String desc = (user.getDong() != null && !user.getDong().isBlank()
                ? user.getDong() + " "
                : "") + p.label + " 이용 영수증을 업로드하면 자동 인증됩니다.";

        return UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(p)
                .title(title)
                .description(desc)
                .verificationType(VerificationType.RECEIPT_OCR)
                .minAmount(t.minAmount())
                .rewardPoint(t.rewardPoint())
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .build();
    }

    // ===================== 기존 로직들 =====================
    public List<UserMission> listCustomMissions(User user) {
        return repo.findByUserAndCategoryOrderByCreatedAtAsc(user, MissionCategory.CUSTOM);
    }

    public UserMission getUserMission(User user, Long missionId) {
        return repo.findByIdAndUser(missionId, user)
                .orElseThrow(() -> new IllegalArgumentException("미션이 없거나 권한이 없습니다."));
    }

    public UserMission start(User user, Long missionId) {
        UserMission m = getUserMission(user, missionId);
        if (m.getStatus() != MissionStatus.READY) {
            throw new IllegalStateException("READY 상태에서만 시작할 수 있습니다.");
        }
        m.setStatus(MissionStatus.IN_PROGRESS);
        m.setStartedAt(LocalDateTime.now());
        return repo.save(m);
    }

    public UserMission completeAuto(User user, Long missionId, Long receiptIdIfAny) {
        UserMission m = getUserMission(user, missionId);
        if (m.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 완료할 수 있습니다.");
        }

        VerificationType vt = m.getVerificationType();
        if (vt == VerificationType.PHOTO) {
            m.setStatus(MissionStatus.COMPLETED);
            m.setCompletedAt(LocalDateTime.now());
            return repo.save(m);
        }

        if (vt == VerificationType.RECEIPT_OCR) {
            if (receiptIdIfAny == null) {
                throw new IllegalArgumentException("receiptId는 필수입니다. (영수증 인증 미션)");
            }
            return completeByReceipt(user, missionId, receiptIdIfAny);
        }

        throw new IllegalStateException("지원하지 않는 인증 방식입니다: " + vt);
    }

    public UserMission complete(User user, Long missionId, VerificationType type) {
        UserMission m = getUserMission(user, missionId);
        if (m.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 완료할 수 있습니다.");
        }
        m.setStatus(MissionStatus.COMPLETED);
        m.setCompletedAt(LocalDateTime.now());
        return repo.save(m);
    }

    @Transactional
    public UserMission completeByReceipt(User user, Long missionId, Long receiptId) {
        UserMission m = getUserMission(user, missionId);
        if (m.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 완료할 수 있습니다.");
        }
        if (m.getVerificationType() != VerificationType.RECEIPT_OCR) {
            throw new IllegalStateException("이 미션은 영수증 인증 미션이 아닙니다.");
        }

        Receipt r = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("영수증이 없습니다."));
        if (r.getUser() == null || r.getUser().getId() == null ||
                !r.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인 영수증이 아닙니다.");
        }
        if (r.getUserMission() == null || !r.getUserMission().getId().equals(m.getId())) {
            throw new IllegalStateException("해당 미션의 영수증이 아닙니다.");
        }
        if (r.getOcrStatus() != OcrStatus.SUCCEEDED) {
            throw new IllegalStateException("OCR 처리가 완료되지 않았습니다.");
        }

        boolean categoryMatched = (r.getDetectedPlaceCategory() != null
                && r.getDetectedPlaceCategory() == m.getPlaceCategory());

        Integer amt = r.getAmount();
        boolean amountSatisfied = (amt != null && amt >= m.getMinAmount());

        boolean inPeriod = !checkPeriod || isWithinPeriod(r.getPurchaseAt(), m.getStartDate(), m.getEndDate());

        if (categoryMatched && amountSatisfied && inPeriod) {
            m.setStatus(MissionStatus.COMPLETED);
            m.setCompletedAt(LocalDateTime.now());

            if (r.getVerificationStatus() != VerificationStatus.MATCHED) {
                r.setVerificationStatus(VerificationStatus.MATCHED);
                r.setRejectReason(null);
                receiptRepository.save(r);
            }

            UserMission saved = repo.save(m);

            // 완료 카운트 → 3개 단위 시 오케스트레이터 호출
            int completed = repo.countCompletedByUser(user);
            if (completed % 3 == 0) {
                aiMissionOrchestrator.recommendNextSet(user.getId().longValue());
            }

            return saved;
        } else {
            String reason = String.format(
                    "categoryMatched=%s, amountSatisfied=%s, inPeriod=%s (detected=%s, mission=%s, amt=%s/%s, purchaseAt=%s, periodCheck=%s)",
                    categoryMatched, amountSatisfied, inPeriod,
                    String.valueOf(r.getDetectedPlaceCategory()), m.getPlaceCategory(),
                    String.valueOf(amt), String.valueOf(m.getMinAmount()),
                    String.valueOf(r.getPurchaseAt()), checkPeriod);
            throw new IllegalStateException("VERIFICATION_FAILED: " + reason);
        }
    }

    private boolean isWithinPeriod(LocalDateTime purchaseAt, LocalDate start, LocalDate end) {
        if (purchaseAt == null)
            return true;
        LocalDate d = purchaseAt.toLocalDate();
        boolean afterOrEqStart = (start == null) || !d.isBefore(start);
        boolean beforeOrEqEnd = (end == null) || !d.isAfter(end);
        return afterOrEqStart && beforeOrEqEnd;
    }

    /**
     * 포기 시 "처음 상태"로 되돌리기
     * - 허용 상태: IN_PROGRESS, ABANDONED(중복요청 방지 위해 재초기화 허용)
     * - 결과: READY, startedAt/completedAt null
     */
    public UserMission abandon(User user, Long missionId) {
        UserMission m = getUserMission(user, missionId);
        if (m.getStatus() != MissionStatus.IN_PROGRESS && m.getStatus() != MissionStatus.ABANDONED) {
            throw new IllegalStateException("진행 중 또는 이미 포기된 미션에서만 초기화할 수 있습니다.");
        }
        m.resetToReady();
        return repo.save(m);
    }

    // ============================================================
    // ============ 전체/상태별 조회(레포 수정 없이) =================
    // ============================================================

    public List<UserMission> listAllMissions(User user) {
        List<UserMission> all = new ArrayList<>();
        for (MissionCategory c : MissionCategory.values()) {
            all.addAll(repo.findMissionsByUserAndCategory(user, c));
        }
        all.sort(Comparator.comparing(UserMission::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return all;
    }

    public List<UserMission> listMissionsByStatus(User user, MissionStatus status) {
        if (status == null)
            return listAllMissions(user);
        return listAllMissions(user).stream()
                .filter(m -> m.getStatus() == status)
                .toList();
    }

    public List<Object[]> getMonthlySummary(User user) {
        return receiptRepository.sumAmountAndCountByMonth(user.getId().longValue());
    }

    public List<Object[]> getMonthlyCategorySummary(User user) {
        return receiptRepository.sumAmountByMonthAndCategory(user.getId().longValue());
    }

    public Double getAverageSpending(User user) {
        return receiptRepository.averageAmount(user.getId().longValue());
    }

    /**
     * 사용자의 선호 장소 기반으로 미션 동기화
     * - 빠진 카테고리는 관련 미션 + 영수증 전부 삭제
     * - 새로 추가된 카테고리는 미션 새로 생성
     */
    @Transactional
    public void syncCustomMissions(User user, List<PlaceCategory> newPrefs) {
        // 현재 사용자 커스텀 미션 목록
        List<UserMission> current = repo.findByUserAndCategory(user, MissionCategory.CUSTOM);
        Set<PlaceCategory> currentCats = current.stream()
                .map(UserMission::getPlaceCategory)
                .collect(Collectors.toSet());

        // (1) 삭제 로직 비활성화: 기존 미션은 보존
        // Set<PlaceCategory> toRemove = new HashSet<>(currentCats);
        // toRemove.removeAll(new HashSet<>(newPrefs));
        // for (PlaceCategory cat : toRemove) {
        // List<UserMission> missions = repo.findByUserAndCategoryAndPlaceCategory(user,
        // MissionCategory.CUSTOM, cat);
        // for (UserMission m : missions) {
        // receiptRepository.deleteAllByUserMission(m);
        // repo.delete(m);
        // }
        // }

        // (2) 새로 추가된 카테고리만 생성
        Set<PlaceCategory> toAdd = new HashSet<>(newPrefs);
        toAdd.removeAll(currentCats);

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(14);

        String prefix = (user.getDong() != null && !user.getDong().isBlank()) ? (user.getDong() + " ") : "";

        for (PlaceCategory cat : toAdd) {
            // 템플릿 기반 문장형 2개 생성 (TPL/TPL_ALT)
            Template t1 = TPL.getOrDefault(cat, TPL.get(PlaceCategory.OTHER));
            Template t2 = TPL_ALT.getOrDefault(cat, TPL_ALT.get(PlaceCategory.OTHER));

            String title1 = buildTitle(prefix, t1.pattern(), t1.minAmount());
            String title2 = buildTitle(prefix, t2.pattern(), t2.minAmount());

            repo.save(buildMission(user, cat, title1, t1, start, end));
            repo.save(buildMission(user, cat, title2, t2, start, end));
        }
    }
}
