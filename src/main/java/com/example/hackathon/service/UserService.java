// src/main/java/com/example/hackathon/service/UserService.java
package com.example.hackathon.service;

import com.example.hackathon.dto.auth.LoginRequest;
import com.example.hackathon.dto.auth.LoginResponse;
import com.example.hackathon.dto.auth.SignUpRequest;
import com.example.hackathon.entity.Background;
import com.example.hackathon.entity.CharacterEntity;
import com.example.hackathon.entity.CharacterKind;
import com.example.hackathon.entity.User;
import com.example.hackathon.entity.UserBackground;
import com.example.hackathon.entity.UserCharacterProgress;
import com.example.hackathon.entity.UserSkin;
import com.example.hackathon.repository.BackgroundRepository;
import com.example.hackathon.repository.CharacterRepository;
import com.example.hackathon.repository.CharacterSkinRepository;
import com.example.hackathon.repository.UserBackgroundRepository;
import com.example.hackathon.repository.UserCharacterProgressRepository;
import com.example.hackathon.repository.UserRepository;
import com.example.hackathon.repository.UserSkinRepository;
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

    private static final long DEFAULT_BG_ID = 1L;       // 기본 배경(1번)

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

    /** 가입 직후 기본 삐약이/배경 보유·활성·진행도 보장 */
    @Transactional
    protected void initDefaultCharacterForUser(Integer userId) {
        // ---- 0) 기본 배경(1번) 존재 보장 (없으면 생성)
        backgroundRepo.findById(DEFAULT_BG_ID).orElseGet(() -> {
            Background b = new Background();
            // IDENTITY 전략이면 setId가 무시될 수 있으니, 스키마가 허용하지 않으면
            // 미리 마이그레이션으로 기본 배경을 넣어두는 게 가장 안전합니다.
            b.setName("기본 배경");
            b.setPriceCoins(0);
            b.setIsActive(true);
            return backgroundRepo.save(b);
        });

        // ---- 1) 기본 스킨 '삐약이' 찾기 (name 기반)
        Long chickSkinId = skinRepo.findAll().stream()
                .filter(s -> "삐약이".equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기본 스킨 '삐약이'가 스킨 마스터에 없습니다."))
                .getId();

        // ---- 2) 캐릭터 프로필 생성/업데이트: 활성 스킨 = 삐약이, 활성 배경 = 1
        CharacterEntity ch = characterRepo.findByUserId(userId).orElseGet(() -> {
            CharacterEntity c = new CharacterEntity();
            c.setUserId(userId);
            return c;
        });
        ch.setKind(CharacterKind.CHICK);
        ch.setDisplayName("삐약이");          // 레거시 표시명 (실제 표시는 UserCharacterProgress.displayName 사용)
        ch.setActiveSkinId(chickSkinId);
        ch.setActiveBackgroundId(DEFAULT_BG_ID);
        if (ch.getLevel() == null) ch.setLevel(1);
        if (ch.getFeedProgress() == null) ch.setFeedProgress(0);
        characterRepo.save(ch);

        // ---- 3) 배경 소유 인벤토리 보장 (user_backgrounds)
        if (!userBackgroundRepo.existsByUserIdAndBackgroundId(userId, DEFAULT_BG_ID)) {
            UserBackground ub = new UserBackground();
            ub.setUserId(userId);
            ub.setBackgroundId(DEFAULT_BG_ID);
            userBackgroundRepo.save(ub);
        }

        // ---- 4) 스킨 소유 보장 (user_skins)
        if (!userSkinRepo.existsByUserIdAndSkinId(userId, chickSkinId)) {
            UserSkin us = new UserSkin();
            us.setUserId(userId);
            us.setSkinId(chickSkinId);
            userSkinRepo.save(us);
        }

        // ---- 5) 스킨별 진행도/표시명 보장 (user_character_progress)
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
}
