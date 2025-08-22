package com.example.hackathon.ai_custom.service;

import com.example.hackathon.ai_custom.dto.AiMissionResult;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DummyMissionProvider {

    // 더미 미션 저장 (실제라면 DB 조회)
    private static final Map<String, List<AiMissionResult>> dummyMissions = new HashMap<>();

    static {
        dummyMissions.put("CAFE", List.of(
                new AiMissionResult(1L, "CAFE", "카페에서 아메리카노 마시기", "근처 카페에서 아메리카노 주문 후 인증"),
                new AiMissionResult(2L, "CAFE", "카페에서 독서하기", "카페에서 책을 읽는 사진 인증")
        ));
        dummyMissions.put("RESTAURANT", List.of(
                new AiMissionResult(3L, "RESTAURANT", "식당에서 점심 식사", "식당에서 한 끼 인증"),
                new AiMissionResult(4L, "RESTAURANT", "새로운 음식 도전", "평소 안 먹던 메뉴 먹기")
        ));
        dummyMissions.put("PARK", List.of(
                new AiMissionResult(5L, "PARK", "공원 산책하기", "공원 걷는 사진 인증"),
                new AiMissionResult(6L, "PARK", "운동하기", "공원에서 운동 인증샷")
        ));
        // ... 나머지도 같은 방식으로 채우면 됨
    }

    public List<AiMissionResult> pickMissions(List<String> categories) {
        List<AiMissionResult> picked = new ArrayList<>();
        long idCounter = 100L;

        for (String cat : categories) {
            List<AiMissionResult> pool = dummyMissions.getOrDefault(cat, List.of());
            if (!pool.isEmpty()) {
                picked.add(pool.get(new Random().nextInt(pool.size())));
            }
            if (picked.size() == 3) break;
        }

        // 3개 못 채웠을 경우 보정
        while (picked.size() < 3) {
            picked.add(new AiMissionResult(idCounter++, "OTHER", "기타 활동", "다양한 체험 인증"));
        }

        return picked;
    }
}
