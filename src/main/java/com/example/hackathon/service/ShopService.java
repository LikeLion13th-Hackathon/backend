package com.example.hackathon.service;

import com.example.hackathon.common.ForbiddenException;
import com.example.hackathon.common.InsufficientBalanceException;
import com.example.hackathon.common.NotFoundException;
import com.example.hackathon.dto.BackgroundDTO;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.dto.ShopOverviewDTO;
import com.example.hackathon.entity.*;
import com.example.hackathon.repository.*;
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

  private int feedsRequired(int level) {
    return levelRepo.findById(level)
        .map(CharacterLevelRequirement::getFeedsRequired) // override 우선
        .orElseGet(() -> feedsRequiredFormula(level));    // 없으면 공식
  }

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

  @Transactional(readOnly = true)
  public List<BackgroundDTO> listBackgrounds(Integer userId) {
    Set<Long> owned = userBgRepo.findByUserId(userId).stream()
        .map(UserBackground::getBackgroundId).collect(Collectors.toSet());
    Long activeBg = characterRepo.findByUserId(userId)
        .map(CharacterEntity::getActiveBackgroundId).orElse(null);

    return backgroundRepo.findAllActive().stream()
        .map(b -> new BackgroundDTO(
            b.getId(), b.getName(), b.getPriceCoins(),
            owned.contains(b.getId()), Objects.equals(activeBg, b.getId())
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BackgroundDTO> listInventory(Integer userId) {
    Long activeBg = characterRepo.findByUserId(userId)
        .map(CharacterEntity::getActiveBackgroundId).orElse(null);

    Map<Long, Background> catalog = backgroundRepo.findAllById(
        userBgRepo.findByUserId(userId).stream().map(UserBackground::getBackgroundId).toList()
    ).stream().collect(Collectors.toMap(Background::getId, it -> it));

    return userBgRepo.findByUserId(userId).stream()
        .map(ub -> {
          Background b = catalog.get(ub.getBackgroundId());
          return new BackgroundDTO(b.getId(), b.getName(), b.getPriceCoins(),
              true, Objects.equals(activeBg, b.getId()));
        })
        .toList();
  }

  @Transactional
  public void activateBackground(Integer userId, Long backgroundId) {
    userBgRepo.findByUserIdAndBackgroundId(userId, backgroundId)
        .orElseThrow(() -> new ForbiddenException("not owned"));
    CharacterEntity ch = characterRepo.findByUserId(userId)
        .orElseThrow(() -> new NotFoundException("character"));
    ch.setActiveBackgroundId(backgroundId);
    characterRepo.save(ch);
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
}