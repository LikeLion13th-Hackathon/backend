// src/main/java/com/example/hackathon/controller/ShopController.java
package com.example.hackathon.controller;

import com.example.hackathon.auth.CurrentUserResolver;
import com.example.hackathon.dto.BackgroundDTO;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.dto.SkinDTO;
import com.example.hackathon.dto.ShopOverviewDTO;
import com.example.hackathon.service.CoinService;
import com.example.hackathon.service.ShopService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.hackathon.dto.SkinActivateResult;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final CoinService coinService;
    private final CurrentUserResolver currentUser; // ✅ 요청에서 userId 해석

    // 공통 유저 식별 헬퍼
    private Integer uid(HttpServletRequest request) {
        return currentUser.resolveUserId(request);
    }

    // ===== 오버뷰 =====
    @GetMapping("/overview")
    public ShopOverviewDTO overview(HttpServletRequest request) {
        return shopService.getOverview(uid(request));
    }

    // ===== 코인 =====
    /** 코인 잔액 단독 조회 (배지 갱신 등에서 사용) */
    @GetMapping("/coins")
    public Map<String, Object> getMyCoins(HttpServletRequest request) {
        Integer userId = uid(request);
        int balance = coinService.getBalanceByUserId(userId);
        return Map.of("userId", userId, "balance", balance);
    }

    // ===== 배경 =====
    @GetMapping("/backgrounds")
    public List<BackgroundDTO> listBackgrounds(HttpServletRequest request) {
        return shopService.listBackgrounds(uid(request));
    }

    @GetMapping("/inventory/backgrounds")
    public List<BackgroundDTO> listInventory(HttpServletRequest request) {
        return shopService.listInventory(uid(request));
    }

    // 배경 구매 → BackgroundDTO 반환
    @PostMapping("/purchase/background")
    public BackgroundDTO purchaseBackground(@RequestParam Long backgroundId,
                                            HttpServletRequest request) {
        return shopService.purchaseBackground(uid(request), backgroundId);
    }

    @PatchMapping("/backgrounds/{backgroundId}/activate")
    public BackgroundDTO activateBackground(@PathVariable Long backgroundId,
                                            HttpServletRequest request) {
        return shopService.activateBackground(uid(request), backgroundId);
    }

    // ===== 먹이 =====
    @PostMapping("/feed")
    public CharacterInfoDTO feed(HttpServletRequest request) {
        return shopService.feedOnce(uid(request));
    }

    // ===== 스킨 =====
    // 스킨 구매 → SkinDTO 반환
    @PostMapping("/purchase/skin")
    public SkinDTO purchaseSkin(@RequestParam Long skinId,
                                HttpServletRequest request) {
        return shopService.purchaseSkin(uid(request), skinId);
    }

    @GetMapping("/skins")
    public List<SkinDTO> listSkins(HttpServletRequest request) {
        return shopService.listSkins(uid(request));
    }

    @GetMapping("/inventory/skins")
    public List<SkinDTO> listSkinInventory(HttpServletRequest request) {
        return shopService.listSkinInventory(uid(request));
    }

    @PatchMapping("/skins/{skinId}/activate")
    public SkinDTO activateSkin(@PathVariable Long skinId,
                                HttpServletRequest request) {
        return shopService.activateSkin(uid(request), skinId);
    }




    // ===== 캐릭터 =====
    @PatchMapping("/character/name")
    public CharacterInfoDTO updateCharacterName(@RequestParam String newName,
                                                HttpServletRequest request) {
        return shopService.updateCharacterName(uid(request), newName);
    }

    @PostMapping("/evolve")
    public CharacterInfoDTO evolve(HttpServletRequest request) {
        return shopService.evolve(uid(request));
    }
}
