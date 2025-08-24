package com.example.hackathon.ai_custom.service;

import com.example.hackathon.ai_custom.dto.GeneratedMission;
import com.example.hackathon.mission.entity.PlaceCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMissionOrchestratorImpl implements AiMissionOrchestrator {

    private final MissionPatternAnalyzer missionPatternAnalyzer;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final MissionWriter missionWriter;

    @Override
    @Transactional
    public void recommendNextSet(Long userId) {
        log.info("[AI] recommendNextSet START userId={}", userId);

        MissionPatternAnalyzer.Result pat = missionPatternAnalyzer.analyze(userId, 3);
        List<PlaceCategory> top = new ArrayList<>(pat.getTopCategories());
        fillTopWithFallbacks(top);

        PlaceCategory newCategory = pickNewCategory(top);

        String prompt = promptBuilder.buildPrompt(userId, top, pat.getPeakHourBand());
        prompt += "\n\n다음 제약을 지켜주세요.\n" +
                "1) 총 4개 항목을 생성합니다.\n" +
                "2) 첫 3개 항목의 category는 각각 순서대로 " + top.get(0) + ", " + top.get(1) + ", " + top.get(2) + " 입니다.\n" +
                "3) 마지막 1개 항목의 category는 반드시 " + newCategory + " 입니다.\n" +
                "4) 각 항목은 title, description, category, minAmount, rewardPoint를 포함합니다.\n" +
                "5) category는 ENUM 이름과 정확히 일치(CAFE, RESTAURANT, ...).\n";

        List<GeneratedMission> generated = geminiClient.generateMissions(prompt);
        List<GeneratedMission> fixed = coerceToFour(generated, top, newCategory);

        missionWriter.saveMissions(userId, fixed);

        log.info("[AI] recommendNextSet DONE userId={}, created={}", userId, fixed.size());
    }

    // 이하 유틸 메서드 그대로 (fillTopWithFallbacks, pickNewCategory, coerceToFour 등)...
    private void fillTopWithFallbacks(List<PlaceCategory> top) {
        PlaceCategory[] defaults = {PlaceCategory.CAFE, PlaceCategory.RESTAURANT, PlaceCategory.OTHER};
        int i = 0;
        while (top.size() < 3 && i < defaults.length) {
            if (!top.contains(defaults[i])) top.add(defaults[i]);
            i++;
        }
        while (top.size() < 3) top.add(PlaceCategory.OTHER);
    }

    private PlaceCategory pickNewCategory(List<PlaceCategory> top) {
        for (PlaceCategory pc : PlaceCategory.values()) {
            if (!top.contains(pc)) return pc;
        }
        return PlaceCategory.OTHER;
    }

    private List<GeneratedMission> coerceToFour(List<GeneratedMission> missions,
                                                List<PlaceCategory> top,
                                                PlaceCategory newCategory) {
        List<GeneratedMission> out = new ArrayList<>(4);
        if (missions == null) missions = List.of();
        List<GeneratedMission> firstFour = new ArrayList<>(missions.stream().limit(4).toList());
        while (firstFour.size() < 4) {
            firstFour.add(GeneratedMission.builder()
                    .title("AI 미션 " + (firstFour.size() + 1))
                    .description("영수증 인증 미션을 완료해보세요!")
                    .placeCategory(PlaceCategory.OTHER)
                    .minAmount(0)
                    .rewardPoint(100)
                    .build());
        }

        for (int i = 0; i < 3; i++) {
            GeneratedMission gm = firstFour.get(i);
            PlaceCategory forced = top.get(i);
            out.add(GeneratedMission.builder()
                    .title(nonEmpty(gm.getTitle(), defaultTitleFor(forced, i + 1)))
                    .description(nonEmpty(gm.getDescription(), defaultDescFor(forced)))
                    .placeCategory(forced)
                    .minAmount(gm.getMinAmount() != null ? gm.getMinAmount() : 0)
                    .rewardPoint(gm.getRewardPoint() != null ? gm.getRewardPoint() : 120)
                    .build());
        }

        GeneratedMission gm4 = firstFour.get(3);
        out.add(GeneratedMission.builder()
                .title(nonEmpty(gm4.getTitle(), defaultTitleFor(newCategory, 4)))
                .description(nonEmpty(gm4.getDescription(), defaultDescFor(newCategory)))
                .placeCategory(newCategory)
                .minAmount(gm4.getMinAmount() != null ? gm4.getMinAmount() : 0)
                .rewardPoint(gm4.getRewardPoint() != null ? gm4.getRewardPoint() : 150)
                .build());

        return out;
    }

    private String nonEmpty(String s, String fb) {
        return (s == null || s.isBlank()) ? fb : s;
    }

    private String defaultTitleFor(PlaceCategory c, int idx) {
        return switch (c) {
            case CAFE -> "카페 루틴 바꾸기 #" + idx + " ☕→🍵";
            case RESTAURANT -> "동네 맛집 재발견 #" + idx;
            case MUSEUM -> "문화 충전 미션 #" + idx;
            case LIBRARY -> "지식 채우기 미션 #" + idx;
            case PARK -> "바람 쐬고 힐링 #" + idx;
            case SPORTS_FACILITY -> "땀 한 방울 챌린지 #" + idx;
            case SHOPPING_MALL -> "알뜰 쇼핑 챌린지 #" + idx;
            case TRADITIONAL_MARKET -> "시장 투어 챌린지 #" + idx;
            case OTHER -> "우리동네 랜덤 미션 #" + idx;
        };
    }

    private String defaultDescFor(PlaceCategory c) {
        return switch (c) {
            case CAFE -> "이번엔 건강차/허브티로 오후 루틴 바꿔보기! 영수증 인증 📷";
            case RESTAURANT -> "평소 가던 곳 대신 미개척 1곳 방문하고 영수증 인증!";
            case MUSEUM -> "근처 전시/박물관 관람 후 영수증을 인증해요.";
            case LIBRARY -> "서점/도서관에서 작은 소비 또는 대출 영수증 인증!";
            case PARK -> "공원/편의시설에서 간단 소비 인증으로 힐링!";
            case SPORTS_FACILITY -> "근처 체육시설 이용권 결제 후 영수증 인증!";
            case SHOPPING_MALL -> "필요한 물건만 스마트 쇼핑! 영수증 인증";
            case TRADITIONAL_MARKET -> "시장 한 바퀴, 소소한 소비 영수증 인증";
            case OTHER -> "우리동네에서 의미 있는 한 번의 소비를 인증해요.";
        };
    }
}
