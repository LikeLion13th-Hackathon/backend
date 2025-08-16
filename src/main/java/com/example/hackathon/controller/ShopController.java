// src/main/java/com/example/hackathon/controller/ShopController.java
package com.example.hackathon.controller;

import com.example.hackathon.dto.BackgroundDTO;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.dto.ShopOverviewDTO;
import com.example.hackathon.entity.Background;
import com.example.hackathon.entity.CharacterEntity;
import com.example.hackathon.repository.BackgroundRepository;
import com.example.hackathon.repository.CharacterRepository;
import com.example.hackathon.security.UserPrincipal;
import com.example.hackathon.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

  private final ShopService shopService;
  private final BackgroundRepository backgroundRepo;
  private final CharacterRepository characterRepo;

  /** 상점 상단 정보 */
  @GetMapping("/overview")
  public ShopOverviewDTO overview(@AuthenticationPrincipal UserPrincipal me) {
    return shopService.getOverview(me.id());
  }

  /** 판매 중 배경 목록 */
  @GetMapping("/backgrounds")
  public List<BackgroundDTO> listBackgrounds(@AuthenticationPrincipal UserPrincipal me) {
    return shopService.listBackgrounds(me.id());
  }

  /** 내가 가진 배경 목록 */
  @GetMapping("/inventory/backgrounds")
  public List<BackgroundDTO> listInventory(@AuthenticationPrincipal UserPrincipal me) {
    return shopService.listInventory(me.id());
  }

  /** 배경 구매 → 구매한 배경 정보 반환 */
  @PostMapping("/purchase/background")
  public ResponseEntity<BackgroundDTO> purchase(
          @AuthenticationPrincipal UserPrincipal me,
          @RequestParam Long backgroundId
  ) {
    shopService.purchaseBackground(me.id(), backgroundId);

    var bg = backgroundRepo.findById(backgroundId).orElseThrow();
    var activeId = characterRepo.findByUserId(me.id())
            .map(CharacterEntity::getActiveBackgroundId)
            .orElse(null);

    BackgroundDTO dto = new BackgroundDTO(
            bg.getId(),
            bg.getName(),
            bg.getPriceCoins(),
            true, // 방금 구매했으므로 보유 = true
            Objects.equals(activeId, backgroundId)
    );

    return ResponseEntity.ok(dto);
  }

  /** 배경 활성화 → 최신 캐릭터 상태 반환 */
  @PatchMapping("/backgrounds/{backgroundId}/activate")
  public ResponseEntity<CharacterInfoDTO> activate(
          @AuthenticationPrincipal UserPrincipal me,
          @PathVariable Long backgroundId
  ) {
    shopService.activateBackground(me.id(), backgroundId);
    return ResponseEntity.ok(shopService.getCharacterInfo(me.id()));
  }

  /** 먹이 주기 */
  @PostMapping("/feed")
  public CharacterInfoDTO feed(@AuthenticationPrincipal UserPrincipal me) {
    return shopService.feedOnce(me.id());
  }
}