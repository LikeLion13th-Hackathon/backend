// src/main/java/com/example/hackathon/service/CharacterQueryService.java
package com.example.hackathon.service;

import com.example.hackathon.common.NotFoundException;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.entity.CharacterEntity;
import com.example.hackathon.entity.CharacterKind;
import com.example.hackathon.entity.CharacterLevelRequirement;
import com.example.hackathon.entity.CharacterSkin;
import com.example.hackathon.entity.UserCharacterProgress;
import com.example.hackathon.repository.CharacterRepository;
import com.example.hackathon.repository.LevelReqRepository;
import com.example.hackathon.repository.CharacterSkinRepository;
import com.example.hackathon.repository.UserCharacterProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CharacterQueryService {

    private final CharacterRepository characterRepo;
    private final LevelReqRepository levelRepo;
    private final CharacterSkinRepository skinRepo;
    private final UserCharacterProgressRepository progressRepo;

    // ===== 내부 유틸 =====
    private int feedsRequiredFormula(int level) {
        if (level <= 0 || level > 30) throw new IllegalArgumentException("level out of range");
        return (int)((1L << level) - 1L); // 2^L - 1
    }

    private int feedsRequired(int level) {
        return levelRepo.findById(level)
                .map(CharacterLevelRequirement::getFeedsRequired)
                .orElseGet(() -> feedsRequiredFormula(level));
    }

    /** 레벨별 타이틀 매핑 */
    private String resolveTitle(CharacterKind kind, int level) {
        if (kind == null) kind = CharacterKind.CHICK;

        if (kind == CharacterKind.CHICK) {
            if (level <= 1) return "호기심많은 삐약이";
            if (level == 2) return "활발한 삐약이";
            return "용맹한 삐약이";
        } else { // CAT
            if (level <= 1) return "얌전한 야옹이";
            if (level == 2) return "새침한 야옹이";
            return "도도한 야옹이";
        }
    }

    // ===== 캐릭터 조회 =====
    @Transactional(readOnly = true)
    public CharacterInfoDTO getCharacterInfo(Integer userId) {
        CharacterEntity ch = characterRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("character"));

        Long activeSkinId = ch.getActiveSkinId();
        CharacterKind kind = CharacterKind.CHICK;
        int level = 1;
        int feedProgress = 0;
        String displayName = "삐약이";

        if (activeSkinId != null) {
            CharacterSkin skin = skinRepo.findById(activeSkinId).orElse(null);
            if (skin != null && skin.getKind() != null) {
                kind = skin.getKind();
                displayName = skin.getName();
            }

            UserCharacterProgress p = progressRepo.findByUserIdAndSkinId(userId, activeSkinId)
                    .orElse(null);
            if (p != null) {
                level = p.getLevel();
                feedProgress = p.getFeedProgress();
                if (p.getDisplayName() != null && !p.getDisplayName().isBlank()) {
                    displayName = p.getDisplayName();
                }
            }
        }

        int required = feedsRequired(level);
        int toNext = Math.max(0, required - feedProgress);
        String title = resolveTitle(kind, level);

        return CharacterInfoDTO.builder()
                .skinId(activeSkinId)
                .level(level)
                .feedProgress(feedProgress)
                .feedsRequiredToNext(toNext)
                .activeBackgroundId(ch.getActiveBackgroundId())
                .title(title)
                .displayName(displayName)
                .build();
    }

}
