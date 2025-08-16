// src/main/java/com/example/hackathon/controller/ShopController.java
package com.example.hackathon.controller;

import com.example.hackathon.dto.BackgroundDTO;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.dto.ShopOverviewDTO;
import com.example.hackathon.security.UserPrincipal;
import com.example.hackathon.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 컨트롤러는 오직 @AuthenticationPrincipal 로 현재 유저만 받아서 넘기면 됨.
 * 별도의 SecurityContext/DB 조회 불필요.
 */
@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

  private final ShopService shopService;

  @GetMapping("/overview")
  public ShopOverviewDTO overview(@AuthenticationPrincipal UserPrincipal me) {
    return shopService.getOverview(me.id());
  }

  @GetMapping("/backgrounds")
  public List<BackgroundDTO> listBackgrounds(@AuthenticationPrincipal UserPrincipal me) {
    return shopService.listBackgrounds(me.id());
  }

  @GetMapping("/inventory/backgrounds")
  public List<BackgroundDTO> listInventory(@AuthenticationPrincipal UserPrincipal me) {
    return shopService.listInventory(me.id());
  }

  @PostMapping("/purchase/background")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void purchase(@AuthenticationPrincipal UserPrincipal me,
                       @RequestParam Long backgroundId) {
    shopService.purchaseBackground(me.id(), backgroundId);
  }

  @PatchMapping("/backgrounds/{backgroundId}/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void activate(@AuthenticationPrincipal UserPrincipal me,
                       @PathVariable Long backgroundId) {
    shopService.activateBackground(me.id(), backgroundId);
  }

  @PostMapping("/feed")
  public CharacterInfoDTO feed(@AuthenticationPrincipal UserPrincipal me) {
    return shopService.feedOnce(me.id());
  }
}
