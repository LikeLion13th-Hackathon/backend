// src/main/java/com/example/hackathon/service/UserService.java
package com.example.hackathon.service;

import com.example.hackathon.dto.auth.LoginRequest;
import com.example.hackathon.dto.auth.LoginResponse;
import com.example.hackathon.dto.auth.SignUpRequest;
import com.example.hackathon.entity.*;
import com.example.hackathon.repository.*;
import com.example.hackathon.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 기본 캐릭터/스킨/진행도/배경 초기화를 위해 주입
    private final CharacterSkinRepository skinRepo;
    private final CharacterRepository characterRepo;
    private final UserSkinRepository userSkinRepo;
    private final UserCharacterProgressRepository progressRepo;
    private final BackgroundRepository backgroundRepo;
    private final UserBackgroundRepository userBackgroundRepo;

    // 기본 배경/스킨 ID
    private static final long DEFAULT_BG_ID = 1L;   // 배경 1번 (기본 배경)

    @Transactional
    public Integer register(SignUpRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String hash = passwordEncoder.encode(req.password());
        User saved = userRepository.save(
                User.builder()
                        .nickname(req.nickname())
                        .email(req.email())
                        .passwordHash(hash)
                        .birthDate(req.birthDate())
                        .sido(req.sido())
                        .sigungu(req.sigungu())
                        .dong(req.dong())
                        .role(req.role())
                        .pref1(req.pref1())
                        .pref2(req.pref2())
                        .pref3(req.pref3())
                        .locationConsent(req.locationConsent())
                        .marketingConsent(req.marketingConsent())
                        .serviceAgreed(req.serviceAgreed())
                        .privacyAgreed(req.privacyAgreed())
                        .build()
        );

        // 가입 직후 기본 삐약이/배경 지급·활성 및 진행도 보장
        initDefaultCharacterForUser(saved.getId());

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole());
        return new LoginResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                token,
                "Bearer",
                "로그인 성공"
        );
    }

    @Transactional
    protected void initDefaultCharacterForUser(Integer userId) {
        // === 0) 기본 배경(1,2) 보장 ===
        ensureBackgroundExists(1L, "기본 배경1");
        ensureBackgroundExists(2L, "기본 배경2");

        // === 1) 기본 스킨 '삐약이' 찾기 ===
        Long chickSkinId = skinRepo.findAll().stream()
                .filter(s -> "삐약이".equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기본 스킨 '삐약이'가 스킨 마스터에 없습니다."))
                .getId();

        // === 2) 캐릭터 프로필 생성/업데이트 ===
        CharacterEntity ch = characterRepo.findByUserId(userId).orElseGet(() -> {
            CharacterEntity c = new CharacterEntity();
            c.setUserId(userId);
            return c;
        });
        ch.setKind(CharacterKind.CHICK);
        ch.setDisplayName("삐약이");       // 레거시용
        ch.setActiveSkinId(chickSkinId);  // 삐약이 활성화
        ch.setActiveBackgroundId(1L);     // 배경 1번 활성화
        if (ch.getLevel() == null) ch.setLevel(1);
        if (ch.getFeedProgress() == null) ch.setFeedProgress(0);
        characterRepo.save(ch);

        // === 3) 배경 보유 보장 ===
        giveBackgroundIfNotOwned(userId, 1L);
        giveBackgroundIfNotOwned(userId, 2L);

        // === 4) 스킨 보유 보장 ===
        if (!userSkinRepo.existsByUserIdAndSkinId(userId, chickSkinId)) {
            UserSkin us = new UserSkin();
            us.setUserId(userId);
            us.setSkinId(chickSkinId);
            userSkinRepo.save(us);
        }

        // === 5) 스킨별 진행도 보장 ===
        if (!progressRepo.existsByUserIdAndSkinId(userId, chickSkinId)) {
            UserCharacterProgress p = new UserCharacterProgress();
            p.setUserId(userId);
            p.setSkinId(chickSkinId);
            p.setLevel(1);
            p.setFeedProgress(0);
            p.setDisplayName("삐약이"); // 기본 표시명
            progressRepo.save(p);
        }
    }

    // === 헬퍼 메서드들 ===
    private void ensureBackgroundExists(Long id, String defaultName) {
        backgroundRepo.findById(id).orElseGet(() -> {
            Background b = new Background();
            b.setName(defaultName);
            b.setPriceCoins(0);
            b.setIsActive(true);
            return backgroundRepo.save(b);
        });
    }

    private void giveBackgroundIfNotOwned(Integer userId, Long backgroundId) {
        if (!userBackgroundRepo.existsByUserIdAndBackgroundId(userId, backgroundId)) {
            UserBackground ub = new UserBackground();
            ub.setUserId(userId);
            ub.setBackgroundId(backgroundId);
            userBackgroundRepo.save(ub);
        }
    }

}
