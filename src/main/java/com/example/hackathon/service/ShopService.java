// src/main/java/com/example/hackathon/service/ShopService.java
package com.example.hackathon.service;

import com.example.hackathon.common.ForbiddenException;
import com.example.hackathon.common.InsufficientBalanceException;
import com.example.hackathon.common.NotFoundException;
import com.example.hackathon.dto.BackgroundDTO;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.dto.SkinDTO;
import com.example.hackathon.dto.ShopOverviewDTO;
import com.example.hackathon.entity.Background;
import com.example.hackathon.entity.CharacterEntity;
import com.example.hackathon.entity.CharacterKind;
import com.example.hackathon.entity.CharacterLevelRequirement;
import com.example.hackathon.entity.CharacterSkin;
import com.example.hackathon.entity.Coin;
import com.example.hackathon.entity.UserBackground;
import com.example.hackathon.entity.UserCharacterProgress;
import com.example.hackathon.entity.UserSkin;
import com.example.hackathon.repository.BackgroundRepository;
import com.example.hackathon.repository.CharacterRepository;
import com.example.hackathon.repository.CharacterSkinRepository;
import com.example.hackathon.repository.CoinsRepository;
import com.example.hackathon.repository.LevelReqRepository;
import com.example.hackathon.repository.UserBackgroundRepository;
import com.example.hackathon.repository.UserCharacterProgressRepository;
import com.example.hackathon.repository.UserSkinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopService {

  private final CharacterRepository characterRepo;
  private final BackgroundRepository backgroundRepo;
  private final UserBackgroundRepository userBgRepo;
  private final LevelReqRepository levelRepo;
  private final CoinsRepository coinsRepo;

  // 스킨
  private final CharacterSkinRepository skinRepo;
  private final UserSkinRepository userSkinRepo;

  // 스킨별 진행도/표시이름
  private final UserCharacterProgressRepository progressRepo;

  // ✅ 공용 캐릭터 조회 (홈/상점 동일 소스)
  private final CharacterQueryService characterQueryService;

  private static final int FEED_COST = 100; // 먹이 1회 = 100코인

  // -------------------- 내부 유틸 --------------------
  private int feedsRequiredFormula(int level) {
    if (level <= 0 || level > 30) throw new IllegalArgumentException("level out of range");
    return (int) ((1L << level) - 1L); // 2^L - 1
  }

  private int feedsRequired(int level) {
    return levelRepo.findById(level)
            .map(CharacterLevelRequirement::getFeedsRequired)
            .orElseGet(() -> feedsRequiredFormula(level));
  }

  /** 현재 레벨에서 다음 레벨로 가는 진화 비용 */
  private int evolveCost(int currentLevel) {
    if (currentLevel == 1) return 300; // 1 -> 2
    if (currentLevel == 2) return 500; // 2 -> 3
    throw new ForbiddenException("cannot evolve from current level");
  }

  private int currentBalance(Integer userId) {
    return coinsRepo.findByUser_Id(userId).map(Coin::getBalance).orElse(0);
  }

  private BackgroundDTO toBackgroundDTO(Background b, boolean owned, boolean active) {
    // 조회/목록용 DTO (balance 없음)
    return new BackgroundDTO(b.getId(), b.getName(), b.getPriceCoins(), owned, active);
  }

  private SkinDTO toSkinDTO(CharacterSkin s, boolean owned, boolean active) {
    // 조회/목록용 DTO (balance 없음)
    return new SkinDTO(s.getId(), s.getName(), s.getPriceCoins(), owned, active);
  }

  /** 스킨 종류에 따른 기본 이름 */
  private String defaultNameByKind(CharacterKind kind) {
    return (kind == CharacterKind.CAT) ? "야옹이" : "삐약이";
  }

  /** 스킨별 진행도/표시이름 row 보장 */
  @Transactional
  public void ensureProgress(Integer userId, Long skinId) {
    if (skinId == null) return;
    if (progressRepo.existsByUserIdAndSkinId(userId, skinId)) return;

    CharacterSkin skin = skinRepo.findById(skinId)
            .orElseThrow(() -> new NotFoundException("skin"));

    UserCharacterProgress p = new UserCharacterProgress();
    p.setUserId(userId);
    p.setSkinId(skinId);
    p.setLevel(1);
    p.setFeedProgress(0);
    // 기본 표시이름은 스킨 종류별로
    CharacterKind kind = skin.getKind() == null ? CharacterKind.CHICK : skin.getKind();
    p.setDisplayName(defaultNameByKind(kind));
    progressRepo.save(p);
  }

  // -------------------- 상단 개요/캐릭터 --------------------
  @Transactional(readOnly = true)
  public ShopOverviewDTO getOverview(Integer userId) {
    CharacterInfoDTO info = characterQueryService.getCharacterInfo(userId);
    return ShopOverviewDTO.builder()
            .character(info)
            .build();
  }

  @Transactional(readOnly = true)
  public CharacterInfoDTO getCharacterInfo(Integer userId) {
    return characterQueryService.getCharacterInfo(userId);
  }

  // -------------------- 먹이 / 진화 --------------------
  /** 먹이 1회: 활성 스킨의 게이지만 증가(레벨업 X), required로 캡 */
  @Transactional
  public CharacterInfoDTO feedOnce(Integer userId) {
    if (coinsRepo.tryDeduct(userId, FEED_COST) == 0) {
      throw new InsufficientBalanceException();
    }

    CharacterEntity ch = characterRepo.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("character"));
    Long activeSkinId = ch.getActiveSkinId();
    if (activeSkinId == null) throw new NotFoundException("active skin not set");

    // 스킨별 진행도 보장 후 로딩
    ensureProgress(userId, activeSkinId);
    UserCharacterProgress p = progressRepo.findByUserIdAndSkinId(userId, activeSkinId)
            .orElseThrow(() -> new NotFoundException("progress"));

    int required = feedsRequired(p.getLevel());
    int newProgress = Math.min(required, p.getFeedProgress() + 1);
    p.setFeedProgress(newProgress);
    progressRepo.save(p);

    return characterQueryService.getCharacterInfo(userId);
  }

  /** 진화: 활성 스킨의 게이지 100% 필요, 레벨별 코인 차감 후 레벨 +1 & 게이지 0 */
  @Transactional
  public CharacterInfoDTO evolve(Integer userId) {
    CharacterEntity ch = characterRepo.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("character"));
    Long activeSkinId = ch.getActiveSkinId();
    if (activeSkinId == null) throw new NotFoundException("active skin not set");

    ensureProgress(userId, activeSkinId);
    UserCharacterProgress p = progressRepo.findByUserIdAndSkinId(userId, activeSkinId)
            .orElseThrow(() -> new NotFoundException("progress"));

    int required = feedsRequired(p.getLevel());
    if (p.getFeedProgress() < required) {
      throw new ForbiddenException("not ready to evolve");
    }

    int cost = evolveCost(p.getLevel());
    if (coinsRepo.tryDeduct(userId, cost) == 0) {
      throw new InsufficientBalanceException();
    }

    p.setLevel(p.getLevel() + 1);
    p.setFeedProgress(0);
    progressRepo.save(p);

    return characterQueryService.getCharacterInfo(userId);
  }

  // -------------------- 배경 --------------------
  @Transactional(readOnly = true)
  public List<BackgroundDTO> listBackgrounds(Integer userId) {
    Set<Long> owned = userBgRepo.findByUserId(userId).stream()
            .map(UserBackground::getBackgroundId)
            .collect(Collectors.toSet());

    Long activeBg = characterRepo.findByUserId(userId)
            .map(CharacterEntity::getActiveBackgroundId)
            .orElse(null);

    return backgroundRepo.findAllActive().stream()
            .map(b -> toBackgroundDTO(b, owned.contains(b.getId()), Objects.equals(activeBg, b.getId())))
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<BackgroundDTO> listInventory(Integer userId) {
    Long activeBg = characterRepo.findByUserId(userId)
            .map(CharacterEntity::getActiveBackgroundId)
            .orElse(null);

    List<Long> ownedIds = userBgRepo.findByUserId(userId).stream()
            .map(UserBackground::getBackgroundId)
            .collect(Collectors.toList());

    Map<Long, Background> catalog = backgroundRepo.findAllById(ownedIds).stream()
            .collect(Collectors.toMap(Background::getId, it -> it));

    return ownedIds.stream()
            .map(id -> {
              Background b = catalog.get(id);
              return toBackgroundDTO(b, true, Objects.equals(activeBg, b.getId()));
            })
            .collect(Collectors.toList());
  }

  /** 배경 구매 → 성공 시 BackgroundDTO(잔액 포함) 반환 */
  @Transactional
  public BackgroundDTO purchaseBackground(Integer userId, Long backgroundId) {
    Background bg = backgroundRepo.findById(backgroundId)
            .filter(Background::getIsActive)
            .orElseThrow(() -> new NotFoundException("background"));

    Long activeBg = characterRepo.findByUserId(userId)
            .map(CharacterEntity::getActiveBackgroundId)
            .orElse(null);

    // 이미 보유면 idempotent → 잔액 포함 반환
    if (userBgRepo.existsByUserIdAndBackgroundId(userId, backgroundId)) {
      return new BackgroundDTO(
              bg.getId(), bg.getName(), bg.getPriceCoins(),
              true, Objects.equals(activeBg, bg.getId()), currentBalance(userId)
      );
    }

    if (coinsRepo.tryDeduct(userId, bg.getPriceCoins()) == 0) {
      throw new InsufficientBalanceException();
    }

    UserBackground ub = new UserBackground();
    ub.setUserId(userId);
    ub.setBackgroundId(backgroundId);
    userBgRepo.save(ub);

    return new BackgroundDTO(
            bg.getId(), bg.getName(), bg.getPriceCoins(),
            true, Objects.equals(activeBg, bg.getId()), currentBalance(userId)
    );
  }

  /** 보유 배경 활성화 → 성공 시 BackgroundDTO 반환 (잔액 불필요) */
  @Transactional
  public BackgroundDTO activateBackground(Integer userId, Long backgroundId) {
    userBgRepo.findByUserIdAndBackgroundId(userId, backgroundId)
            .orElseThrow(() -> new ForbiddenException("not owned"));

    CharacterEntity ch = characterRepo.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("character"));

    ch.setActiveBackgroundId(backgroundId);
    characterRepo.save(ch);

    Background bg = backgroundRepo.findById(backgroundId)
            .orElseThrow(() -> new NotFoundException("background"));

    return new BackgroundDTO(bg.getId(), bg.getName(), bg.getPriceCoins(), true, true);
  }

  // -------------------- 스킨(캐릭터) --------------------
  @Transactional(readOnly = true)
  public List<SkinDTO> listSkins(Integer userId) {
    Set<Long> owned = userSkinRepo.findByUserId(userId).stream()
            .map(UserSkin::getSkinId)
            .collect(Collectors.toSet());

    Long activeSkin = characterRepo.findByUserId(userId)
            .map(CharacterEntity::getActiveSkinId)
            .orElse(null);

    return skinRepo.findAllActive().stream()
            .map(s -> toSkinDTO(s, owned.contains(s.getId()), Objects.equals(activeSkin, s.getId())))
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<SkinDTO> listSkinInventory(Integer userId) {
    Long activeSkin = characterRepo.findByUserId(userId)
            .map(CharacterEntity::getActiveSkinId)
            .orElse(null);

    List<Long> ownedIds = userSkinRepo.findByUserId(userId).stream()
            .map(UserSkin::getSkinId)
            .collect(Collectors.toList());

    Map<Long, CharacterSkin> catalog = skinRepo.findAllById(ownedIds).stream()
            .collect(Collectors.toMap(CharacterSkin::getId, it -> it));

    return ownedIds.stream()
            .map(id -> {
              CharacterSkin s = catalog.get(id);
              return toSkinDTO(s, true, Objects.equals(activeSkin, s.getId()));
            })
            .collect(Collectors.toList());
  }

  /** 스킨 구매 → 성공 시 SkinDTO(잔액 포함) 반환 + 진행도/표시이름 보장 */
  @Transactional
  public SkinDTO purchaseSkin(Integer userId, Long skinId) {
    CharacterSkin skin = skinRepo.findById(skinId)
            .filter(CharacterSkin::getIsActive)
            .orElseThrow(() -> new NotFoundException("skin"));

    Long activeSkin = characterRepo.findByUserId(userId)
            .map(CharacterEntity::getActiveSkinId)
            .orElse(null);

    // 이미 보유면 idempotent → 잔액 포함 반환
    if (userSkinRepo.existsByUserIdAndSkinId(userId, skinId)) {
      return new SkinDTO(
              skin.getId(), skin.getName(), skin.getPriceCoins(),
              true, Objects.equals(activeSkin, skin.getId()), currentBalance(userId)
      );
    }

    if (coinsRepo.tryDeduct(userId, skin.getPriceCoins()) == 0) {
      throw new InsufficientBalanceException();
    }

    UserSkin us = new UserSkin();
    us.setUserId(userId);
    us.setSkinId(skinId);
    userSkinRepo.save(us);

    // 스킨별 진행도/표시이름 보장 (기본 표시이름: 삐약이/야옹이)
    ensureProgress(userId, skinId);

    return new SkinDTO(
            skin.getId(), skin.getName(), skin.getPriceCoins(),
            true, Objects.equals(activeSkin, skin.getId()), currentBalance(userId)
    );
  }

  /** 보유 스킨 활성화 → 성공 시 SkinDTO 반환 (잔액 불필요) + 진행도 보장 */
  @Transactional
  public SkinDTO activateSkin(Integer userId, Long skinId) {
    userSkinRepo.findByUserIdAndSkinId(userId, skinId)
            .orElseThrow(() -> new ForbiddenException("not owned"));

    CharacterEntity ch = characterRepo.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("character"));

    ch.setActiveSkinId(skinId);
    characterRepo.save(ch);

    // 스킨별 진행도/표시이름 row 보장
    ensureProgress(userId, skinId);

    CharacterSkin skin = skinRepo.findById(skinId)
            .orElseThrow(() -> new NotFoundException("skin"));

    return new SkinDTO(skin.getId(), skin.getName(), skin.getPriceCoins(), true, true);
  }

  // -------------------- 캐릭터 이름 변경 (활성 스킨의 이름 변경) --------------------
  @Transactional
  public CharacterInfoDTO updateCharacterName(Integer userId, String newNameRaw) {
    String newName = newNameRaw == null ? "" : newNameRaw.trim();
    if (newName.isEmpty()) {
      throw new IllegalArgumentException("캐릭터 이름은 비워둘 수 없습니다.");
    }
    if (newName.length() > 20) {
      throw new IllegalArgumentException("캐릭터 이름은 20자 이내여야 합니다.");
    }

    CharacterEntity ch = characterRepo.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("character"));
    Long activeSkinId = ch.getActiveSkinId();
    if (activeSkinId == null) throw new NotFoundException("active skin not set");

    // 진행도 row 생성/로딩 후 표시이름 갱신
    ensureProgress(userId, activeSkinId);
    UserCharacterProgress p = progressRepo.findByUserIdAndSkinId(userId, activeSkinId)
            .orElseThrow(() -> new NotFoundException("progress"));
    p.setDisplayName(newName);
    progressRepo.save(p);

    return characterQueryService.getCharacterInfo(userId);
  }
}
