// src/main/java/com/example/hackathon/service/HomeCardService.java
package com.example.hackathon.service;

import com.example.hackathon.dto.home.HomeCardDTO;
import com.example.hackathon.entity.Background;
import com.example.hackathon.entity.CharacterEntity;
import com.example.hackathon.entity.CharacterLevelRequirement;
import com.example.hackathon.entity.User;
import com.example.hackathon.repository.BackgroundRepository;
import com.example.hackathon.repository.CharacterRepository;
import com.example.hackathon.repository.CoinsRepository;
import com.example.hackathon.repository.LevelReqRepository;
import com.example.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeCardService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final LevelReqRepository levelReqRepository;
    private final CoinsRepository coinsRepository;
    private final BackgroundRepository backgroundRepository;

    public HomeCardDTO getCardByEmail(String email) {
        // 1) 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 2) 코인 잔액 (OneToOne User FK)
        int coins = coinsRepository.findByUser_Id(user.getId())
                .map(c -> c.getBalance() == null ? 0 : c.getBalance())
                .orElse(0);

        // 3) 캐릭터 조회 (유저당 1개 가정)
        CharacterEntity ch = characterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("캐릭터가 없습니다."));

        int level = ch.getLevel();
        int feedProgress = ch.getFeedProgress();

        // 4) 현재 레벨의 요구 먹이 수 조회 (없으면 0 → 100%)
        int feedsRequired = levelReqRepository.findByLevel(level)
                .map(CharacterLevelRequirement::getFeedsRequired)
                .orElse(0);

        double expPercent = (feedsRequired <= 0)
                ? 100.0
                : Math.min(100.0, (feedProgress * 100.0) / (double) feedsRequired);

        // 소수 1자리로 반올림
        expPercent = Math.round(expPercent * 10) / 10.0;

        // 5) 적용 배경 이름
        String backgroundName = null;
        if (ch.getActiveBackgroundId() != null) {
            backgroundName = backgroundRepository.findById(ch.getActiveBackgroundId())
                    .map(Background::getName)
                    .orElse(null);
        }

        // 6) 응답 DTO
        return HomeCardDTO.builder()
                .coins(coins)
                .characterName("삐약이") // CharacterEntity에 이름 필드가 있으면 ch.getName()으로 변경
                .level(level)
                .expPercent(expPercent)
                .backgroundName(backgroundName)
                .build();
    }
}
