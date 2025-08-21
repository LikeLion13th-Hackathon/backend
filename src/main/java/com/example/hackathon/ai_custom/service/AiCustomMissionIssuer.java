// src/main/java/com/example/hackathon/ai_custom/service/AiCustomMissionIssuer.java
package com.example.hackathon.ai_custom.service;

import com.example.hackathon.ai_custom.entity.MissionTemplate;
import com.example.hackathon.ai_custom.repository.MissionTemplateRepository;
import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.*;
import com.example.hackathon.mission.repository.UserMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AiCustomMissionIssuer {

    private final UserMissionRepository missionRepo;
    private final MissionTemplateRepository templateRepo;

    /**
     * 추천된 place 3개를 받아 AI_CUSTOM 3개 발급(템플릿 랜덤 선택)
     * 기존 READY/ABANDONED AI_CUSTOM은 필요 시 정리(주석 부분)하세요.
     */
    public void refreshAiCustomMissions(User user, List<PlaceCategory> places) {
        // 필요시 정리:
        // missionRepo.deleteByUserAndCategoryAndStatusIn(user, MissionCategory.AI_CUSTOM,
        //        List.of(MissionStatus.READY, MissionStatus.ABANDONED));

        LocalDate start = LocalDate.now();
        LocalDate end   = start.plusWeeks(3);

        for (PlaceCategory pc : places) {
            MissionTemplate tpl = templateRepo.pickRandomMany(pc.name(), PageRequest.of(0,1))
                    .stream().findFirst()
                    .or(() -> templateRepo.pickRandomOne(pc.name()))
                    .orElse(null);

            String title, desc;
            VerificationType vt;
            Integer minAmt, reward;

            if (tpl != null) {
                title  = tpl.getTitle();
                desc   = tpl.getDescription();
                vt     = tpl.getVerificationType();
                minAmt = tpl.getMinAmount();
                reward = tpl.getRewardPoint();
            } else {
                // 템플릿 없을 때 안전 기본값
                title  = pc.label + " AI 맞춤 미션";
                desc   = pc.label + " 이용 시 영수증 인증";
                vt     = VerificationType.RECEIPT_OCR;
                minAmt = 3000;
                reward = 180;
            }

            UserMission m = UserMission.builder()
                    .user(user)
                    .category(MissionCategory.AI_CUSTOM)
                    .placeCategory(pc)
                    .title(title)
                    .description(desc)
                    .verificationType(vt)
                    .minAmount(minAmt)
                    .rewardPoint(reward)
                    .status(MissionStatus.READY)
                    .startDate(start)
                    .endDate(end)
                    .build();

            missionRepo.save(m);
        }
    }
}
