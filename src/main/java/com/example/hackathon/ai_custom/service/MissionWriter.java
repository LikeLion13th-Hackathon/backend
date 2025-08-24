package com.example.hackathon.ai_custom.service;

import com.example.hackathon.ai_custom.dto.GeneratedMission;
import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.entity.VerificationType;
import com.example.hackathon.mission.repository.UserMissionRepository;
import com.example.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissionWriter {

    private final UserRepository userRepository;
    private final UserMissionRepository userMissionRepository;

    public void saveMissions(Long userId, List<GeneratedMission> generated) {
        User user = userRepository.findById(userId.intValue()) // ★ intValue()로 변환
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다. id=" + userId));

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusWeeks(2);

        for (GeneratedMission gm : generated) {
            UserMission mission = UserMission.builder()
                    .user(user)
                    .category(MissionCategory.AI_CUSTOM)
                    .placeCategory(gm.getPlaceCategory())
                    .title(gm.getTitle())
                    .description(gm.getDescription())
                    .verificationType(VerificationType.RECEIPT_OCR)
                    .minAmount(gm.getMinAmount() != null ? gm.getMinAmount() : 0)
                    .rewardPoint(gm.getRewardPoint() != null ? gm.getRewardPoint() : 100)
                    .status(MissionStatus.READY)
                    .startDate(start)
                    .endDate(end)
                    .build();

            userMissionRepository.save(mission);
            log.info("[AI] Saved mission for userId={} title={}", userId, gm.getTitle());
        }
    }
}
