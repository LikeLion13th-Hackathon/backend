// src/main/java/com/example/hackathon/service/HomeCardService.java
package com.example.hackathon.service;

import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.dto.home.HomeCardResponseDTO;
import com.example.hackathon.repository.CoinsRepository;
import com.example.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeCardService {

    private final UserRepository userRepository;
    private final CoinsRepository coinsRepository;

    // 상점과 홈이 같은 캐릭터 계산을 쓰도록 공용 서비스 사용
    private final CharacterQueryService characterQueryService;

    public HomeCardResponseDTO getCardByEmail(String email) {
        // 1) 유저
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 2) 코인
        int coins = coinsRepository.findByUser_Id(user.getId())
                .map(c -> c.getBalance() == null ? 0 : c.getBalance())
                .orElse(0);

        // 3) 캐릭터 정보
        CharacterInfoDTO characterInfo = characterQueryService.getCharacterInfo(user.getId());

        // 4) 진행 퍼센트 계산
        double progressPercent = 0.0;
        if (characterInfo.getFeedsRequiredToNext() > 0) {
            progressPercent = (characterInfo.getFeedProgress() * 100.0) / characterInfo.getFeedsRequiredToNext();
        }

        // 5) 응답 DTO 생성
        return HomeCardResponseDTO.builder()
                .coins(coins)
                .level(characterInfo.getLevel())
                .displayName(characterInfo.getDisplayName())
                .progressPercent(progressPercent)
                .activeCharacterId(characterInfo.getSkinId())
                .activeBackgroundId(characterInfo.getActiveBackgroundId())
                .build();
    }
}
