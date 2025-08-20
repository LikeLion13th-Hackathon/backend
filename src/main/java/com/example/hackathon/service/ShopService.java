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
import com.example.hackathon.entity.CharacterLevelRequirement;
import com.example.hackathon.entity.CharacterSkin;
import com.example.hackathon.entity.UserBackground;
import com.example.hackathon.entity.UserSkin;
import com.example.hackathon.repository.BackgroundRepository;
import com.example.hackathon.repository.CharacterRepository;
import com.example.hackathon.repository.CharacterSkinRepository;
import com.example.hackathon.repository.CoinsRepository;
import com.example.hackathon.repository.LevelReqRepository;
import com.example.hackathon.repository.UserBackgroundRepository;
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

  private static final int FEED_COST = 100; // 먹이 1회 = 100코인

  // -------------------- 유틸 --------------------
  private int feedsRequiredFormula(int level) {
    if (level <= 0 || level > 30) throw new IllegalArgumentException("level out of range");
    return (int) ((1L << level) - 1L); // 2^L - 1
  }

  private int feedsRequired(int level) {
    return levelRepo.findById(level)
            .map(CharacterLevelRequirement::getFeedsRequired)
            .orElseGet(() -> feedsRequiredFormula(level));
  }

  private BackgroundDTO toBackgroundDTO(Background b, boolean owned, boolean active) {
    return new BackgroundDTO(b.getId(), b.getName(), b.getPriceCoins(), owned, active);
  }

  private SkinDTO toSkinDTO(CharacterSkin s, boolean owned, boolean active) {
    return new SkinDTO(s.getId(), s.getName(), s.getPriceCoins(), owned, active);
  }

  // -------------------- 상단 개요/캐릭터 --------------------
  @Transactional(readOnly = true)
  public ShopOverviewDTO getOverview(Integer userId) {
    CharacterEntity ch = characterRepo.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("character"));
    int required = feedsRequired(ch.getLevel());
    int toNext = Math.max(0, required - ch.getFeedProgress());
    return new ShopOverviewDTO(
            new CharacterInfoDTO(ch.getLevel(), ch.getFeedProgress(), toNext, ch.getActiveBackgroundId())
    );
  }

  @Transactional
  public CharacterInfoDTO feedOnce(Integer userId) {
    if (coinsRepo.tryDeduct(userId, FEED_COST) == 0) {
      throw new InsufficientBalanceException();
    }

    CharacterEntity ch = characterRepo.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("character"));

    int required = feedsRequired(ch.getLevel());
    int progress = ch.getFeedProgress() + 1;

    if (progress >= required) {
      ch.setLevel(ch.getLevel() + 1);
      ch.setFeedProgress(0);
    } else {
      ch.setFeedProgress(progress);
    }
    characterRepo.save(ch);

    int nextReq = feedsRequired(ch.getLevel());
    int toNext = Math.max(0, nextReq - ch.getFeedProgress());
    return new CharacterInfoDTO(ch.getLevel(), ch.getFeedProgress(), toNext, ch.getActiveBackgroundId());
  }

  @Transactional(readOnly = true)
  public CharacterInfoDTO getCharacterInfo(Integer userId) {
    CharacterEntity ch = characterRepo.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("character"));

    int required = feedsRequired(ch.getLevel());
    int toNext = Math.max(0, required - ch.getFeedProgress());
    return new CharacterInfoDTO(ch.getLevel(), ch.getFeedProgress(), toNext, ch.getActiveBackgroundId());
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

  /** 배경 구매 → 성공 시 BackgroundDTO 반환 */
  @Transactional
  public BackgroundDTO purchaseBackground(Integer userId, Long backgroundId) {
    Background bg = backgroundRepo.findById(backgroundId)
            .filter(Background::getIsActive)
            .orElseThrow(() -> new NotFoundException("background"));

    Long activeBg = characterRepo.findByUserId(userId)
            .map(CharacterEntity::getActiveBackgroundId)
            .orElse(null);

    // 이미 보유면 idempotent: 그대로 DTO 반환
    if (userBgRepo.existsByUserIdAndBackgroundId(userId, backgroundId)) {
      return toBackgroundDTO(bg, true, Objects.equals(activeBg, bg.getId()));
    }

    if (coinsRepo.tryDeduct(userId, bg.getPriceCoins()) == 0) {
      throw new InsufficientBalanceException();
    }

    UserBackground ub = new UserBackground();
    ub.setUserId(userId);
    ub.setBackgroundId(backgroundId);
    userBgRepo.save(ub);

    return toBackgroundDTO(bg, true, Objects.equals(activeBg, bg.getId()));
  }

  /** 보유 배경 활성화 → 성공 시 BackgroundDTO 반환 */
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

  /** 스킨 구매 → 성공 시 SkinDTO 반환 */
  @Transactional
  public SkinDTO purchaseSkin(Integer userId, Long skinId) {
    CharacterSkin skin = skinRepo.findById(skinId)
            .filter(CharacterSkin::getIsActive)
            .orElseThrow(() -> new NotFoundException("skin"));

    Long activeSkin = characterRepo.findByUserId(userId)
            .map(CharacterEntity::getActiveSkinId)
            .orElse(null);

    // 이미 보유면 idempotent: 그대로 DTO 반환
    if (userSkinRepo.existsByUserIdAndSkinId(userId, skinId)) {
      return toSkinDTO(skin, true, Objects.equals(activeSkin, skin.getId()));
    }

    if (coinsRepo.tryDeduct(userId, skin.getPriceCoins()) == 0) {
      throw new InsufficientBalanceException();
    }

    UserSkin us = new UserSkin();
    us.setUserId(userId);
    us.setSkinId(skinId);
    userSkinRepo.save(us);

    return toSkinDTO(skin, true, Objects.equals(activeSkin, skin.getId()));
  }

  /** 보유 스킨 활성화 → 성공 시 SkinDTO 반환 */
  @Transactional
  public SkinDTO activateSkin(Integer userId, Long skinId) {
    userSkinRepo.findByUserIdAndSkinId(userId, skinId)
            .orElseThrow(() -> new ForbiddenException("not owned"));

    CharacterEntity ch = characterRepo.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("character"));

    ch.setActiveSkinId(skinId);
    characterRepo.save(ch);

    CharacterSkin skin = skinRepo.findById(skinId)
            .orElseThrow(() -> new NotFoundException("skin"));

    return new SkinDTO(skin.getId(), skin.getName(), skin.getPriceCoins(), true, true);
  }
}
