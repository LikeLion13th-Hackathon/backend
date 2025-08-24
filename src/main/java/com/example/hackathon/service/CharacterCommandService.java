// src/main/java/com/example/hackathon/service/CharacterCommandService.java
package com.example.hackathon.service;

import com.example.hackathon.entity.CharacterEntity;
import com.example.hackathon.entity.CharacterKind;
import com.example.hackathon.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CharacterCommandService {

    private final CharacterRepository characterRepo;

    // 회원가입 시 기본 캐릭터(삐약이) 생성. 이미 있으면 그대로 둠(idempotent)
    @Transactional
    public CharacterEntity ensureDefaultCharacter(Integer userId) {
        return characterRepo.findByUserId(userId).orElseGet(() -> {
            CharacterEntity ch = new CharacterEntity();
            ch.setUserId(userId);
            ch.setLevel(1);
            ch.setFeedProgress(0);
            ch.setKind(CharacterKind.CHICK);       // 삐약이
            ch.setDisplayName("삐약이");
            // activeBackgroundId / activeSkinId는 null 시작
            return characterRepo.save(ch);
        });
    }
}
