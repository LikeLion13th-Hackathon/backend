// package com.example.hackathon.mission.service;

// import com.example.hackathon.entity.User;
// import com.example.hackathon.mission.entity.MissionCategory;
// import com.example.hackathon.mission.entity.MissionStatus;
// import com.example.hackathon.mission.entity.PlaceCategory;
// import com.example.hackathon.mission.entity.UserMission;
// import com.example.hackathon.mission.entity.VerificationType;
// import com.example.hackathon.mission.repository.UserMissionRepository;
// import com.example.hackathon.receipt.OcrStatus;
// import com.example.hackathon.receipt.VerificationStatus;
// import com.example.hackathon.receipt.entity.Receipt;
// import com.example.hackathon.receipt.repository.ReceiptRepository;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.EnumMap;
// import java.util.List;
// import java.util.Map;

// @Service
// @Transactional
// public class MissionService {

//     private final UserMissionRepository repo;
//     private final ReceiptRepository receiptRepository;

//     // AI 의존성
//     private final com.example.hackathon.ai_custom.service.AiRecommendationService aiRecommendationService;
//     private final com.example.hackathon.ai_custom.service.AiCustomMissionIssuer aiCustomMissionIssuer;

//     // 데모용: 기간 체크 X
//     @Value("${app.mission.verify.period:false}")
//     private boolean checkPeriod;

//     public MissionService(UserMissionRepository repo,
//                           ReceiptRepository receiptRepository,
//                           com.example.hackathon.ai_custom.service.AiRecommendationService aiRecommendationService,
//                           com.example.hackathon.ai_custom.service.AiCustomMissionIssuer aiCustomMissionIssuer) {
//         this.repo = repo;
//         this.receiptRepository = receiptRepository;
//         this.aiRecommendationService = aiRecommendationService;
//         this.aiCustomMissionIssuer = aiCustomMissionIssuer;
//     }

//     // 카테고리별 템플릿 (맞춤 미션)
//     private static final Map<PlaceCategory, Template> TPL = new EnumMap<>(PlaceCategory.class);
//     static {
//         put(PlaceCategory.CAFE,               "카페에서 %s원 이상 결제하기",             3000, 200);
//         put(PlaceCategory.RESTAURANT,         "음식점에서 %s원 이상 결제하기",           7000, 250);
//         put(PlaceCategory.MUSEUM,             "박물관/미술관 입장권 결제 영수증 인증하기", 3000, 250);
//         put(PlaceCategory.LIBRARY,            "도서관(부대시설 포함) 결제 영수증 인증하기", 2000, 150);
//         put(PlaceCategory.PARK,               "공원 나들이 후 간식/음료 영수증 인증하기", 2000, 150);
//         put(PlaceCategory.SPORTS_FACILITY,    "운동 시설 이용권 결제 영수증 인증하기",      10000, 200);
//         put(PlaceCategory.SHOPPING_MALL,      "쇼핑센터에서 %s원 이상 결제하기",          8000, 250);
//         put(PlaceCategory.TRADITIONAL_MARKET, "전통 시장에서 %s원 이상 결제하기",          5000, 250);
//         put(PlaceCategory.OTHER,              "주변 상점에서 %s원 이상 결제하기",          3000, 150);
//     }
//     private static void put(PlaceCategory c, String p, int min, int r){ TPL.put(c, new Template(p,min,r)); }
//     private record Template(String pattern, int minAmount, int rewardPoint) {}

//     // ✅ 회원가입 직후 초기 맞춤 미션(장소당 2개씩 생성 -> 총 6개) 
//     public void ensureInitialMissions(User user, List<PlaceCategory> prefs) {
//         if (repo.existsByUser(user)) return;
//         LocalDate start = LocalDate.now();
//         LocalDate end = start.plusWeeks(3);

//         for (PlaceCategory p : prefs) {
//             Template t = TPL.getOrDefault(p, TPL.get(PlaceCategory.OTHER));
//             String prefix = (user.getDong()!=null && !user.getDong().isBlank()) ? (user.getDong()+" ") : "";

//             // 1) 영수증 인증 미션
//             String title1  = prefix + t.pattern().formatted(t.minAmount());
//             String desc1   = prefix + p.label + " 이용 영수증을 업로드하면 자동 인증됩니다.";
//             UserMission m1 = UserMission.builder()
//                     .user(user)
//                     .category(MissionCategory.CUSTOM)
//                     .placeCategory(p)
//                     .title(title1)
//                     .description(desc1)
//                     .verificationType(VerificationType.RECEIPT_OCR)
//                     .minAmount(t.minAmount())
//                     .rewardPoint(t.rewardPoint())
//                     .status(MissionStatus.READY)
//                     .startDate(start)
//                     .endDate(end)
//                     .build();
//             repo.save(m1);

//             // 2) 사진 인증 미션 (더미 추가)
//             String title2 = prefix + p.label + " 사진 인증하기";
//             String desc2  = prefix + p.label + " 방문 후 사진을 업로드하면 인증됩니다.";
//             UserMission m2 = UserMission.builder()
//                     .user(user)
//                     .category(MissionCategory.CUSTOM)
//                     .placeCategory(p)
//                     .title(title2)
//                     .description(desc2)
//                     .verificationType(VerificationType.PHOTO)
//                     .minAmount(null)
//                     .rewardPoint(100) // 사진 인증은 고정 100점
//                     .status(MissionStatus.READY)
//                     .startDate(start)
//                     .endDate(end)
//                     .build();
//             repo.save(m2);
//         }
//     }

//     public List<UserMission> listCustomMissions(User user) {
//         return repo.findByUserAndCategoryOrderByCreatedAtAsc(user, MissionCategory.CUSTOM);
//     }

//     public UserMission getUserMission(User user, Long missionId) {
//         return repo.findByIdAndUser(missionId, user)
//                 .orElseThrow(() -> new IllegalArgumentException("미션이 없거나 권한이 없습니다."));
//     }

//     public UserMission start(User user, Long missionId) {
//         UserMission m = getUserMission(user, missionId);
//         if (m.getStatus() != MissionStatus.READY) {
//             throw new IllegalStateException("READY 상태에서만 시작할 수 있습니다.");
//         }
//         m.setStatus(MissionStatus.IN_PROGRESS);
//         m.setStartedAt(LocalDateTime.now());
//         return m;
//     }

//     // PHOTO: 즉시 완료 / RECEIPT_OCR: receiptId 필요
//     public UserMission completeAuto(User user, Long missionId, Long receiptIdIfAny) {
//         UserMission m = getUserMission(user, missionId);
//         if (m.getStatus() != MissionStatus.IN_PROGRESS) {
//             throw new IllegalStateException("IN_PROGRESS 상태에서만 완료할 수 있습니다.");
//         }

//         VerificationType vt = m.getVerificationType();
//         if (vt == VerificationType.PHOTO) {
//             m.setStatus(MissionStatus.COMPLETED);
//             m.setCompletedAt(LocalDateTime.now());
//             maybeTriggerAi(user); // ✅ 트리거
//             return m;
//         }

//         if (vt == VerificationType.RECEIPT_OCR) {
//             if (receiptIdIfAny == null) {
//                 throw new IllegalArgumentException("receiptId는 필수입니다. (영수증 인증 미션)");
//             }
//             UserMission done = completeByReceipt(user, missionId, receiptIdIfAny);
//             maybeTriggerAi(user); // ✅ 트리거
//             return done;
//         }

//         throw new IllegalStateException("지원하지 않는 인증 방식입니다: " + vt);
//     }

//     public UserMission complete(User user, Long missionId, VerificationType type) {
//         UserMission m = getUserMission(user, missionId);
//         if (m.getStatus() != MissionStatus.IN_PROGRESS) {
//             throw new IllegalStateException("IN_PROGRESS 상태에서만 완료할 수 있습니다.");
//         }
//         m.setStatus(MissionStatus.COMPLETED);
//         m.setCompletedAt(LocalDateTime.now());
//         maybeTriggerAi(user); // ✅ 트리거
//         return m;
//     }

//     // 영수증 기반 완료
//     @Transactional
//     public UserMission completeByReceipt(User user, Long missionId, Long receiptId) {
//         UserMission m = getUserMission(user, missionId);
//         if (m.getStatus() != MissionStatus.IN_PROGRESS) {
//             throw new IllegalStateException("IN_PROGRESS 상태에서만 완료할 수 있습니다.");
//         }
//         if (m.getVerificationType() != VerificationType.RECEIPT_OCR) {
//             throw new IllegalStateException("이 미션은 영수증 인증 미션이 아닙니다.");
//         }

//         Receipt r = receiptRepository.findById(receiptId)
//                 .orElseThrow(() -> new IllegalArgumentException("영수증이 없습니다."));
//         if (r.getUser() == null || r.getUser().getId() == null ||
//                 !r.getUser().getId().equals(user.getId())) {
//             throw new IllegalStateException("본인 영수증이 아닙니다.");
//         }
//         if (r.getUserMission() == null || !r.getUserMission().getId().equals(m.getId())) {
//             throw new IllegalStateException("해당 미션의 영수증이 아닙니다.");
//         }
//         if (r.getOcrStatus() != OcrStatus.SUCCEEDED) {
//             throw new IllegalStateException("OCR 처리가 완료되지 않았습니다.");
//         }

//         boolean categoryMatched =
//                 (r.getDetectedPlaceCategory() != null && r.getDetectedPlaceCategory() == m.getPlaceCategory());

//         Integer amt = r.getAmount();
//         boolean amountSatisfied = (amt != null && amt >= m.getMinAmount());

//         boolean inPeriod = !checkPeriod || isWithinPeriod(r.getPurchaseAt(), m.getStartDate(), m.getEndDate());

//         if (categoryMatched && amountSatisfied && inPeriod) {
//             m.setStatus(MissionStatus.COMPLETED);
//             m.setCompletedAt(LocalDateTime.now());

//             if (r.getVerificationStatus() != VerificationStatus.MATCHED) {
//                 r.setVerificationStatus(VerificationStatus.MATCHED);
//                 r.setRejectReason(null);
//             }
//         } else {
//             String reason = String.format(
//                     "categoryMatched=%s, amountSatisfied=%s, inPeriod=%s (detected=%s, mission=%s, amt=%s/%s, purchaseAt=%s, periodCheck=%s)",
//                     categoryMatched, amountSatisfied, inPeriod,
//                     String.valueOf(r.getDetectedPlaceCategory()), m.getPlaceCategory(),
//                     String.valueOf(amt), String.valueOf(m.getMinAmount()),
//                     String.valueOf(r.getPurchaseAt()), checkPeriod
//             );
//             throw new IllegalStateException("VERIFICATION_FAILED: " + reason);
//         }

//         return m;
//     }

//     private boolean isWithinPeriod(LocalDateTime purchaseAt, LocalDate start, LocalDate end) {
//         if (purchaseAt == null) return true;
//         LocalDate d = purchaseAt.toLocalDate();
//         boolean afterOrEqStart = (start == null) || !d.isBefore(start);
//         boolean beforeOrEqEnd  = (end == null)   || !d.isAfter(end);
//         return afterOrEqStart && beforeOrEqEnd;
//     }

//     public UserMission abandon(User user, Long missionId) {
//         UserMission m = getUserMission(user, missionId);
//         if (m.getStatus() != MissionStatus.IN_PROGRESS) {
//             throw new IllegalStateException("IN_PROGRESS 상태에서만 포기할 수 있습니다.");
//         }
//         m.setStatus(MissionStatus.ABANDONED);
//         return m;
//     }

//     // === AI 트리거 ===
//     private void maybeTriggerAi(User user) {
//         try {
//             long done = repo.countByUserAndStatus(user, MissionStatus.COMPLETED);
//             if (done > 0 && done % 3 == 0) {
//                 var recent = repo.findTop20ByUserAndStatusOrderByCompletedAtDesc(user, MissionStatus.COMPLETED)
//                                  .stream()
//                                  .map(UserMission::getPlaceCategory)
//                                  .toList();
//                 var recs = aiRecommendationService.recommendPlaces(user, recent);
//                 aiCustomMissionIssuer.refreshAiCustomMissions(user, recs);
//             }
//         } catch (Exception e) {
//             // AI 실패는 완료 흐름을 막지 않음
//         }
//     }
// }

package com.example.hackathon.mission.service;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.entity.VerificationType;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class MissionService {

    private final UserMissionRepository repo;
    private final ReceiptRepository receiptRepository;

    // AI 의존성
    private final com.example.hackathon.ai_custom.service.AiRecommendationService aiRecommendationService;
    private final com.example.hackathon.ai_custom.service.AiCustomMissionIssuer aiCustomMissionIssuer;

    // 데모용: 기간 체크 X
    @Value("${app.mission.verify.period:false}")
    private boolean checkPeriod;

    public MissionService(UserMissionRepository repo,
                          ReceiptRepository receiptRepository,
                          com.example.hackathon.ai_custom.service.AiRecommendationService aiRecommendationService,
                          com.example.hackathon.ai_custom.service.AiCustomMissionIssuer aiCustomMissionIssuer) {
        this.repo = repo;
        this.receiptRepository = receiptRepository;
        this.aiRecommendationService = aiRecommendationService;
        this.aiCustomMissionIssuer = aiCustomMissionIssuer;
    }

    // 카테고리별 템플릿 (맞춤 미션)
    private static final Map<PlaceCategory, Template> TPL = new EnumMap<>(PlaceCategory.class);
    static {
        put(PlaceCategory.CAFE,               "카페에서 %s원 이상 결제하기",             3000, 200);
        put(PlaceCategory.RESTAURANT,         "음식점에서 %s원 이상 결제하기",           7000, 250);
        put(PlaceCategory.MUSEUM,             "박물관/미술관 입장권 결제 영수증 인증하기", 3000, 250);
        put(PlaceCategory.LIBRARY,            "도서관(부대시설 포함) 결제 영수증 인증하기", 2000, 150);
        put(PlaceCategory.PARK,               "공원 나들이 후 간식/음료 영수증 인증하기", 2000, 150);
        put(PlaceCategory.SPORTS_FACILITY,    "운동 시설 이용권 결제 영수증 인증하기",      10000, 200);
        put(PlaceCategory.SHOPPING_MALL,      "쇼핑센터에서 %s원 이상 결제하기",          8000, 250);
        put(PlaceCategory.TRADITIONAL_MARKET, "전통 시장에서 %s원 이상 결제하기",          5000, 250);
        put(PlaceCategory.OTHER,              "주변 상점에서 %s원 이상 결제하기",          3000, 150);
    }
    private static void put(PlaceCategory c, String p, int min, int r){ TPL.put(c, new Template(p,min,r)); }
    private record Template(String pattern, int minAmount, int rewardPoint) {}

    // ✅ 회원가입 직후 초기 맞춤 미션(장소당 2개씩 생성 보장)
    public void ensureInitialMissions(User user, List<PlaceCategory> prefs) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusWeeks(3);

        for (PlaceCategory p : prefs) {
            Template t = TPL.getOrDefault(p, TPL.get(PlaceCategory.OTHER));
            String prefix = (user.getDong()!=null && !user.getDong().isBlank()) ? (user.getDong()+" ") : "";

            // 영수증 인증 미션 존재 여부 확인
            boolean hasReceiptMission = repo.existsByUserAndCategoryAndPlaceCategoryAndVerificationType(
                    user, MissionCategory.CUSTOM, p, VerificationType.RECEIPT_OCR);

            if (!hasReceiptMission) {
                String title1  = prefix + t.pattern().formatted(t.minAmount());
                String desc1   = prefix + p.label + " 이용 영수증을 업로드하면 자동 인증됩니다.";
                UserMission m1 = UserMission.builder()
                        .user(user)
                        .category(MissionCategory.CUSTOM)
                        .placeCategory(p)
                        .title(title1)
                        .description(desc1)
                        .verificationType(VerificationType.RECEIPT_OCR)
                        .minAmount(t.minAmount())
                        .rewardPoint(t.rewardPoint())
                        .status(MissionStatus.READY)
                        .startDate(start)
                        .endDate(end)
                        .build();
                repo.save(m1);
            }

            // 사진 인증 미션 존재 여부 확인
            boolean hasPhotoMission = repo.existsByUserAndCategoryAndPlaceCategoryAndVerificationType(
                    user, MissionCategory.CUSTOM, p, VerificationType.PHOTO);

            if (!hasPhotoMission) {
                String title2 = prefix + p.label + " 방문 사진 인증하기";
                String desc2  = prefix + p.label + " 방문 후 사진을 업로드하면 인증됩니다.";
                UserMission m2 = UserMission.builder()
                        .user(user)
                        .category(MissionCategory.CUSTOM)
                        .placeCategory(p)
                        .title(title2)
                        .description(desc2)
                        .verificationType(VerificationType.PHOTO)
                        .minAmount(null)
                        .rewardPoint(100) // 사진 인증은 고정 100점
                        .status(MissionStatus.READY)
                        .startDate(start)
                        .endDate(end)
                        .build();
                repo.save(m2);
            }
        }
    }

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
        return m;
    }

    // PHOTO: 즉시 완료 / RECEIPT_OCR: receiptId 필요
    public UserMission completeAuto(User user, Long missionId, Long receiptIdIfAny) {
        UserMission m = getUserMission(user, missionId);
        if (m.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 완료할 수 있습니다.");
        }

        VerificationType vt = m.getVerificationType();
        if (vt == VerificationType.PHOTO) {
            m.setStatus(MissionStatus.COMPLETED);
            m.setCompletedAt(LocalDateTime.now());
            maybeTriggerAi(user);
            return m;
        }

        if (vt == VerificationType.RECEIPT_OCR) {
            if (receiptIdIfAny == null) {
                throw new IllegalArgumentException("receiptId는 필수입니다. (영수증 인증 미션)");
            }
            UserMission done = completeByReceipt(user, missionId, receiptIdIfAny);
            maybeTriggerAi(user);
            return done;
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
        maybeTriggerAi(user);
        return m;
    }

    // 영수증 기반 완료
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

        boolean categoryMatched =
                (r.getDetectedPlaceCategory() != null && r.getDetectedPlaceCategory() == m.getPlaceCategory());

        Integer amt = r.getAmount();
        boolean amountSatisfied = (amt != null && amt >= m.getMinAmount());

        boolean inPeriod = !checkPeriod || isWithinPeriod(r.getPurchaseAt(), m.getStartDate(), m.getEndDate());

        if (categoryMatched && amountSatisfied && inPeriod) {
            m.setStatus(MissionStatus.COMPLETED);
            m.setCompletedAt(LocalDateTime.now());

            if (r.getVerificationStatus() != VerificationStatus.MATCHED) {
                r.setVerificationStatus(VerificationStatus.MATCHED);
                r.setRejectReason(null);
            }
        } else {
            throw new IllegalStateException("VERIFICATION_FAILED");
        }

        return m;
    }

    private boolean isWithinPeriod(LocalDateTime purchaseAt, LocalDate start, LocalDate end) {
        if (purchaseAt == null) return true;
        LocalDate d = purchaseAt.toLocalDate();
        boolean afterOrEqStart = (start == null) || !d.isBefore(start);
        boolean beforeOrEqEnd  = (end == null)   || !d.isAfter(end);
        return afterOrEqStart && beforeOrEqEnd;
    }

    public UserMission abandon(User user, Long missionId) {
        UserMission m = getUserMission(user, missionId);
        if (m.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 포기할 수 있습니다.");
        }
        m.setStatus(MissionStatus.ABANDONED);
        return m;
    }

    // === AI 트리거 ===
    private void maybeTriggerAi(User user) {
        try {
            long done = repo.countByUserAndStatus(user, MissionStatus.COMPLETED);
            if (done > 0 && done % 3 == 0) {
                var recent = repo.findTop20ByUserAndStatusOrderByCompletedAtDesc(user, MissionStatus.COMPLETED)
                                 .stream()
                                 .map(UserMission::getPlaceCategory)
                                 .toList();
                var recs = aiRecommendationService.recommendPlaces(user, recent);
                aiCustomMissionIssuer.refreshAiCustomMissions(user, recs);
            }
        } catch (Exception e) {
            // AI 실패는 완료 흐름을 막지 않음
        }
    }
}
