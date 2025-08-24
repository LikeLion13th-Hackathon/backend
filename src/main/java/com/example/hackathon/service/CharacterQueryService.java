// src/main/java/com/example/hackathon/service/CharacterQueryService.java
package com.example.hackathon.service;

import com.example.hackathon.common.NotFoundException;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.entity.CharacterEntity;
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
        return (int) ((1L << level) - 1L); // 2^L - 1 (부동소수 오차 X)
    }

    private int feedsRequired(int level) {
        return levelRepo.findById(level)
                .map(CharacterLevelRequirement::getFeedsRequired)
                .orElseGet(() -> feedsRequiredFormula(level));
    }

    @Transactional(readOnly = true)
    public CharacterInfoDTO getCharacterInfo(Integer userId) {
        CharacterEntity ch = characterRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("character"));

        int required = feedsRequired(ch.getLevel());
        int toNext = Math.max(0, required - ch.getFeedProgress());

        return new CharacterInfoDTO(
                ch.getLevel(),
                ch.getFeedProgress(),
                toNext,
                ch.getActiveBackgroundId()
        );
    }
}
