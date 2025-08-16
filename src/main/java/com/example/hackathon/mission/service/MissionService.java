package com.example.hackathon.mission.service;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.*;
import com.example.hackathon.mission.repository.UserMissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class MissionService {

    private final UserMissionRepository repo;

    public MissionService(UserMissionRepository repo) { this.repo = repo; }

    // 카테고리별 템플릿 (맞춤 미션, 영수증 인증)
    private static final Map<PlaceCategory, Template> TPL = new EnumMap<>(PlaceCategory.class);
    static {
        put(PlaceCategory.CAFE,               "카페에서 %s원 이상 결제하기",           3000, 200);
        put(PlaceCategory.RESTAURANT,         "음식점에서 %s원 이상 결제하기",         7000, 250);
        put(PlaceCategory.MUSEUM,             "박물관/미술관 입장권 결제 영수증 인증하기", 3000, 250);
        put(PlaceCategory.LIBRARY,            "도서관(부대시설 포함) 결제 영수증 인증하기", 2000, 150);
        put(PlaceCategory.PARK,               "공원 나들이 후 간식/음료 영수증 인증하기", 2000, 150);
        put(PlaceCategory.SPORTS_FACILITY,    "운동 시설 이용권 결제 영수증 인증하기",    10000, 200);
        put(PlaceCategory.SHOPPING_MALL,      "쇼핑센터에서 %s원 이상 결제하기",        8000, 250);
        put(PlaceCategory.TRADITIONAL_MARKET, "전통 시장에서 %s원 이상 결제하기",        5000, 250);
        put(PlaceCategory.OTHER,              "주변 상점에서 %s원 이상 결제하기",        3000, 150);
    }
    private static void put(PlaceCategory c, String p, int min, int r){ TPL.put(c, new Template(p,min,r)); }
    private record Template(String pattern, int minAmount, int rewardPoint) {}


    // 회원가입 직후 초기 맞춤 미션(3개) 생성 -> 없을 때만
    public void ensureInitialMissions(User user, List<PlaceCategory> prefs) {
        if (repo.existsByUser(user)) return;
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusWeeks(3);

        for (PlaceCategory p : prefs) {
            Template t = TPL.getOrDefault(p, TPL.get(PlaceCategory.OTHER));
            String prefix = (user.getDong()!=null && !user.getDong().isBlank()) ? (user.getDong()+" ") : "";
            String title  = prefix + t.pattern().formatted(t.minAmount());
            String desc   = prefix + p.label + " 이용 영수증을 업로드하면 자동 인증됩니다.";

            UserMission m = UserMission.builder()
                    .user(user)
                    .category(MissionCategory.CUSTOM)
                    .placeCategory(p)
                    .title(title)
                    .description(desc)
                    .verificationType(VerificationType.RECEIPT_OCR) // 초기 규칙
                    .minAmount(t.minAmount())
                    .rewardPoint(t.rewardPoint())
                    .status(MissionStatus.READY)
                    .startDate(start)
                    .endDate(end)
                    .build();

            repo.save(m);
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

    // type 이 null 이거나 어떤 값이든, 지금은 실제 검증 없이 완료만 수행 -> 확장 에정
    public UserMission complete(User user, Long missionId, VerificationType type) {
        UserMission m = getUserMission(user, missionId);

        if (m.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 완료할 수 있습니다.");
        }

        // TODO: 추후 type(RECEIPT_OCR/PHOTO/GPS 등)에 따라 실제 검증 로직 추가
        m.setStatus(MissionStatus.COMPLETED);
        m.setCompletedAt(LocalDateTime.now());
        return m;
    }

    public UserMission abandon(User user, Long missionId) {
        UserMission m = getUserMission(user, missionId);
        if (m.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("IN_PROGRESS 상태에서만 포기할 수 있습니다.");
        }
        m.setStatus(MissionStatus.ABANDONED);
        return m;
    }


}
