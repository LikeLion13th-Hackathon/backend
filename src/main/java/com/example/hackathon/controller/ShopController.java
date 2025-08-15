package com.example.hackathon.controller;

import com.example.hackathon.dto.*;
import com.example.hackathon.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

  private final ShopService shopService;

  // TODO: 실제 인증 붙이면 SecurityContext에서 userId(INT) 추출
  private Integer currentUserId() { return 1; }

  @GetMapping("/overview")
  public ShopOverviewDTO overview() {
    return shopService.getOverview(currentUserId());
  }

  @GetMapping("/backgrounds")
  public List<BackgroundDTO> listBackgrounds() {
    return shopService.listBackgrounds(currentUserId());
  }

  @GetMapping("/inventory/backgrounds")
  public List<BackgroundDTO> listInventory() {
    return shopService.listInventory(currentUserId());
  }

  @PostMapping("/purchase/background")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void purchase(@RequestParam Long backgroundId) {
    shopService.purchaseBackground(currentUserId(), backgroundId);
  }

  @PatchMapping("/backgrounds/{backgroundId}/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void activate(@PathVariable Long backgroundId) {
    shopService.activateBackground(currentUserId(), backgroundId);
  }

  @PostMapping("/feed")
  public CharacterInfoDTO feed() {
    return shopService.feedOnce(currentUserId());
  }
}
