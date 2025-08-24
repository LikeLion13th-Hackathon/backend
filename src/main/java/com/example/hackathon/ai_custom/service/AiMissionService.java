package com.example.hackathon.ai_custom.service;

import com.example.hackathon.ai_custom.dto.AiMissionRequest;
import com.example.hackathon.ai_custom.dto.AiMissionResult;
import com.example.hackathon.ai_custom.repository.AiUserMissionRepository;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMissionService {

    private final MissionPatternAnalyzer missionPatternAnalyzer;
    private final AiUserMissionRepository aiuserMissionRepository;
    private final DummyMissionProvider dummyMissionProvider;

    /**
     * 미션 성공 시 호출되는 메서드
     * - 3개/6개/9개... 완료 시에만 생성
     * - 이미 생성된 세트가 있으면 중복 생성하지 않도록 멱등 처리
     */
    @Transactional
    public void handleMissionSuccess(Long userId) {
        int successCount = aiuserMissionRepository.countByUserIdAndStatusCompleted(userId);

        // 3 이상 & 3의 배수일 때만 생성
        if (successCount < 3 || successCount % 3 != 0) return;

        // 이미 생성된 AI 미션(전체) → 세트 수 환산(1세트=3개 기준)
        int existingAiCount = aiuserMissionRepository.findAiCustomMissions(userId).size();
        int alreadySets = existingAiCount / 3;

        // 이번 완료까지 고려했을 때 필요한 세트 수
        int shouldHaveSets = successCount / 3;

        if (alreadySets >= shouldHaveSets) {
            // 이미 해당 차수까지 생성되어 있음 → 아무 것도 하지 않음(멱등)
            return;
        }

        // 누락 보정 포함, 필요한 세트만큼 생성
        int setsToMake = shouldHaveSets - alreadySets;
        for (int s = 0; s < setsToMake; s++) {
            List<UserMission> completed = aiuserMissionRepository.findCompletedMissions(userId);

            AiMissionRequest request = new AiMissionRequest(
                    completed.stream()
                            .map(m -> m.getPlaceCategory().name()) // UserMission의 enum → "CAFE"
                            .toList()
            );

            // 패턴 분석 → 추천 카테고리 키워드
            List<String> recommendedCategories = missionPatternAnalyzer.analyzePattern(request);

            // 더미 데이터에서 3개 선택
            List<AiMissionResult> picked = dummyMissionProvider.pickMissions(recommendedCategories);

            // DB INSERT (AI_CUSTOM)
            picked.forEach(m -> {
                // m.getCategory()는 String(한글/영어 가능) → enum으로 변환 후 .name()으로 DB ENUM값 맞추기
                PlaceCategory pc = PlaceCategory.from(m.getCategory());
                String placeEnumName = pc.name(); // 예: "CAFE"

                log.info("[AI] INSERT userId={}, placeCategory(enum)={}, title={}",
                        userId, placeEnumName, m.getTitle());

                aiuserMissionRepository.insertAiCustomMission(
                        userId,
                        placeEnumName,          // ★ 반드시 ENUM 이름으로 저장
                        m.getTitle(),
                        m.getDescription()
                );
            });
        }
    }

    /** 사용자 맞춤 미션 3개 조회 */
    @Transactional(readOnly = true)
    public List<AiMissionResult> getUserAiMissions(Long userId) {
        return aiuserMissionRepository.findAiCustomMissions(userId).stream()
                .map(m -> new AiMissionResult(
                        m.getId(),
                        m.getPlaceCategory(),
                        m.getTitle(),
                        m.getDescription()))
                .limit(3)
                .toList();
    }

    /** 맞춤 미션 단건 조회 */
    @Transactional(readOnly = true)
    public AiMissionResult getUserAiMissionById(Long userId, Long missionId) {
        return aiuserMissionRepository.findAiCustomMissionById(userId, missionId)
                .map(m -> new AiMissionResult(
                        m.getId(),
                        m.getPlaceCategory(),
                        m.getTitle(),
                        m.getDescription()))
                .orElseThrow(() -> new RuntimeException("해당 미션 없음"));
    }
}
