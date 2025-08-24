// src/main/java/com/example/hackathon/service/CharacterQueryService.java
package com.example.hackathon.service;

import com.example.hackathon.common.NotFoundException;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.entity.CharacterEntity;
import com.example.hackathon.entity.CharacterKind;
import com.example.hackathon.entity.CharacterLevelRequirement;
import com.example.hackathon.repository.CharacterRepository;
import com.example.hackathon.repository.LevelReqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CharacterQueryService {

    private final CharacterRepository characterRepo;
    private final LevelReqRepository levelRepo;

    private int feedsRequiredFormula(int level) {
        if (level <= 0 || level > 30) throw new IllegalArgumentException("level out of range");
        return (int) ((1L << level) - 1L); // 2^L - 1
    }

    private int feedsRequired(int level) {
        return levelRepo.findById(level)
                .map(CharacterLevelRequirement::getFeedsRequired)
                .orElseGet(() -> feedsRequiredFormula(level));
    }

    /** 레벨별 타이틀 매핑 */
    private String resolveTitle(CharacterKind kind, int level) {
        // 기본값 가드: null이면 삐약이로 처리
        if (kind == null) kind = CharacterKind.CHICK;

        boolean lv1 = level <= 1;
        boolean lv2 = level == 2;
        // 3 이상은 공통(최상위)
        if (kind == CharacterKind.CHICK) {
            if (lv1) return "호기심많은 삐약이";
            if (lv2) return "활발한 삐약이";
            return "용맹한 삐약이";
        } else { // CAT
            if (lv1) return "얌전한 야옹이";
            if (lv2) return "새침한 야옹이";
            return "도도한 야옹이";
        }
    }

    @Transactional(readOnly = true)
    public CharacterInfoDTO getCharacterInfo(Integer userId) {
        CharacterEntity ch = characterRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("character"));

        int required = feedsRequired(ch.getLevel());
        int toNext = Math.max(0, required - ch.getFeedProgress());
        String title = resolveTitle(ch.getKind(), ch.getLevel());

        return new CharacterInfoDTO(
                ch.getId(),                // ★ characterId
                ch.getLevel(),
                ch.getFeedProgress(),
                toNext,
                ch.getActiveBackgroundId(),
                title,                     // ★ 타이틀
                ch.getDisplayName()        // ★ 유저 설정 이름
        );
    }
}
