package com.example.hackathon.service;

import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.dto.home.HomeCardDTO;
import com.example.hackathon.entity.Background;
import com.example.hackathon.repository.BackgroundRepository;
import com.example.hackathon.repository.CoinsRepository;
import com.example.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeCardService {

    private final UserRepository userRepository;
    private final CoinsRepository coinsRepository;
    private final BackgroundRepository backgroundRepository;

    // 상점과 홈이 같은 계산을 쓰도록 공용 서비스 사용
    private final CharacterQueryService characterQueryService;

    public HomeCardDTO getCardByEmail(String email) {
        // 1) 유저
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 2) 코인
        int coins = coinsRepository.findByUser_Id(user.getId())
                .map(c -> c.getBalance() == null ? 0 : c.getBalance())
                .orElse(0);

        // 3) 공용 캐릭터 정보 (level, feedProgress, feedsRequiredToNext, activeBackgroundId)
        CharacterInfoDTO characterInfo = characterQueryService.getCharacterInfo(user.getId());

        // 4) 활성 배경 이름 (표시용)
        String backgroundName = null;
        if (characterInfo.getActiveBackgroundId() != null) {
            backgroundName = backgroundRepository.findById(characterInfo.getActiveBackgroundId())
                    .map(Background::getName)
                    .orElse(null);
        }

        // 5) 응답
        return HomeCardDTO.builder()
                .coins(coins)
                .characterName("삐약이")   // CharacterEntity에 이름 필드가 생기면 교체
                .character(characterInfo) // 상점과 동일 구조
                .backgroundName(backgroundName)
                .build();
    }
}
