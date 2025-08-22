package com.example.hackathon.ai_custom.service;

import com.example.hackathon.ai_custom.dto.AiMissionRequest;
import com.example.hackathon.ai_custom.dto.AiMissionResult;
import com.example.hackathon.ai_custom.repository.AiUserMissionRepository;
import com.example.hackathon.mission.entity.UserMission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiMissionService {

    private final MissionPatternAnalyzer missionPatternAnalyzer;
    private final AiUserMissionRepository aiuserMissionRepository;
    private final DummyMissionProvider dummyMissionProvider;

    /**
     * 미션 성공 시 호출되는 메서드
     */
    @Transactional
    public void handleMissionSuccess(Long userId) {
        int successCount = aiuserMissionRepository.countByUserIdAndStatusCompleted(userId);

        // 3 이상 & 3의 배수일 때만 Gemini 호출
        if (successCount >= 3 && successCount % 3 == 0) {
            List<UserMission> completed = aiuserMissionRepository.findCompletedMissions(userId);

            AiMissionRequest request = new AiMissionRequest(
                    completed.stream()
                            .map(m -> m.getPlaceCategory().name())
                            .toList());

            // Gemini 호출 → 추천 카테고리 리스트
            List<String> recommendedCategories = missionPatternAnalyzer.analyzePattern(request);

            // 더미데이터에서 미션 선택 (3개)
            List<AiMissionResult> picked = dummyMissionProvider.pickMissions(recommendedCategories);

            // DB에 저장 (AI_CUSTOM 카테고리로)
            picked.forEach(m -> aiuserMissionRepository.insertAiCustomMission(
                    userId,
                    m.getCategory(),
                    m.getTitle(),
                    m.getDescription()));
        }
    }

    /**
     * 사용자 맞춤 미션 3개 조회
     */
    @Transactional(readOnly = true)
    public List<AiMissionResult> getUserAiMissions(Long userId) {
        return aiuserMissionRepository.findAiCustomMissions(userId).stream()
                .map(m -> new AiMissionResult(
                        m.getId(),
                        m.getPlaceCategory(),
                        m.getTitle(),
                        m.getDescription()))
                .limit(3) // 혹시 DB에 더 저장돼도 최근 3개만
                .toList();
    }

    /**
     * 맞춤 미션 단건 조회
     */
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
