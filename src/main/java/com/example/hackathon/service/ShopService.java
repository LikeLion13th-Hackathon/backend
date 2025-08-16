package com.example.hackathon.service;

import com.example.hackathon.common.ForbiddenException;
import com.example.hackathon.common.InsufficientBalanceException;
import com.example.hackathon.common.NotFoundException;
import com.example.hackathon.dto.BackgroundDTO;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.dto.ShopOverviewDTO;
import com.example.hackathon.entity.Background;
import com.example.hackathon.entity.CharacterEntity;
import com.example.hackathon.entity.CharacterLevelRequirement;
import com.example.hackathon.entity.UserBackground;
import com.example.hackathon.repository.BackgroundRepository;
import com.example.hackathon.repository.CharacterRepository;
import com.example.hackathon.repository.CoinsRepository;
import com.example.hackathon.repository.LevelReqRepository;
import com.example.hackathon.repository.UserBackgroundRepository;
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

  private static final int FEED_COST = 100; // 먹이 1회 = 100코인

  // 2^L - 1 (L은 현재 레벨), 안전하게 long 비트시프트
  private int feedsRequiredFormula(int level) {
    if (level <= 0 || level > 30) throw new IllegalArgumentException("level out of range");
    return (int) ((1L << level) - 1L);
  }

  // override(있으면) 우선, 없으면 공식(2^L - 1)
  private int feedsRequired(int level) {
    return levelRepo.findById(level)
        .map(CharacterLevelRequirement::getFeedsRequired)
        .orElseGet(() -> feedsRequiredFormula(level));
  }

  /** 상점 상단 정보 */
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

  /** 판매 중 배경 목록 (+내 보유/활성 여부) */
  @Transactional(readOnly = true)
  public List<BackgroundDTO> listBackgrounds(Integer userId) {
    Set<Long> owned = userBgRepo.findByUserId(userId).stream()
        .map(UserBackground::getBackgroundId)
        .collect(Collectors.toSet());

    Long activeBg = characterRepo.findByUserId(userId)
        .map(CharacterEntity::getActiveBackgroundId)
        .orElse(null);

    return backgroundRepo.findAllActive().stream()
        .map(b -> new BackgroundDTO(
            b.getId(), b.getName(), b.getPriceCoins(),
            owned.contains(b.getId()), Objects.equals(activeBg, b.getId())
        ))
        .collect(Collectors.toList());
  }

  /** 내가 소유한 배경 목록 (+활성 여부) */
  @Transactional(readOnly = true)
  public List<BackgroundDTO> listInventory(Integer userId) {
    Long activeBg = characterRepo.findByUserId(userId)
        .map(CharacterEntity::getActiveBackgroundId)
        .orElse(null);

    // 보유 ID 모아서 한 번에 카탈로그 로딩
    List<Long> ownedIds = userBgRepo.findByUserId(userId).stream()
        .map(UserBackground::getBackgroundId)
        .collect(Collectors.toList());

    Map<Long, Background> catalog = backgroundRepo.findAllById(ownedIds).stream()
        .collect(Collectors.toMap(Background::getId, it -> it));

    return ownedIds.stream()
        .map(id -> {
          Background b = catalog.get(id);
          return new BackgroundDTO(
              b.getId(), b.getName(), b.getPriceCoins(),
              true, Objects.equals(activeBg, b.getId())
          );
        })
        .collect(Collectors.toList());
  }

  /** 배경 구매(코인 즉시 차감, 환불 없음) */
  @Transactional
  public void purchaseBackground(Integer userId, Long backgroundId) {
    Background bg = backgroundRepo.findById(backgroundId)
        .filter(Background::getIsActive)
        .orElseThrow(() -> new NotFoundException("background"));

    // 이미 보유 시 no-op (정책에 따라 409로 바꿔도 됨)
    if (userBgRepo.existsByUserIdAndBackgroundId(userId, backgroundId)) return;

    // 코인 조건부 차감 (원자적)
    int updated = coinsRepo.tryDeduct(userId, bg.getPriceCoins());
    if (updated == 0) throw new InsufficientBalanceException();

    // 소유권 등록
    UserBackground ub = new UserBackground();
    ub.setUserId(userId);
    ub.setBackgroundId(backgroundId);
    userBgRepo.save(ub);
  }

  /** 보유 배경 활성화 */
  @Transactional
  public void activateBackground(Integer userId, Long backgroundId) {
    userBgRepo.findByUserIdAndBackgroundId(userId, backgroundId)
        .orElseThrow(() -> new ForbiddenException("not owned"));

    CharacterEntity ch = characterRepo.findByUserId(userId)
        .orElseThrow(() -> new NotFoundException("character"));

    ch.setActiveBackgroundId(backgroundId);
    characterRepo.save(ch);
  }

  /** 먹이 1회(100코인 차감 → 진행 +1 → 필요 시 레벨업) */
  @Transactional
  public CharacterInfoDTO feedOnce(Integer userId) {
    // 1) 코인 차감 (환불 없음)
    if (coinsRepo.tryDeduct(userId, FEED_COST) == 0) {
      throw new InsufficientBalanceException();
    }

    // 2) 진행 업데이트 + 레벨업
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

  // ShopService.java
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

