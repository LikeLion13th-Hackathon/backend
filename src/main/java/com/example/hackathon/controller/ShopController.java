// src/main/java/com/example/hackathon/controller/ShopController.java
package com.example.hackathon.controller;

import com.example.hackathon.dto.BackgroundDTO;
import com.example.hackathon.dto.CharacterInfoDTO;
import com.example.hackathon.dto.SkinDTO;
import com.example.hackathon.dto.ShopOverviewDTO;
import com.example.hackathon.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    // TODO: 실제 서비스에선 @AuthenticationPrincipal 사용
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

    /** 배경 구매 → BackgroundDTO 반환 */
    @PostMapping("/purchase/background")
    public BackgroundDTO purchaseBackground(@RequestParam Long backgroundId) {
        return shopService.purchaseBackground(currentUserId(), backgroundId);
    }

    @PatchMapping("/backgrounds/{backgroundId}/activate")
    public BackgroundDTO activateBackground(@PathVariable Long backgroundId) {
        return shopService.activateBackground(currentUserId(), backgroundId);
    }

    @PostMapping("/feed")
    public CharacterInfoDTO feed() {
        return shopService.feedOnce(currentUserId());
    }

    // ===== 스킨 =====

    /** 스킨 구매 → SkinDTO 반환 */
    @PostMapping("/purchase/skin")
    public SkinDTO purchaseSkin(@RequestParam Long skinId) {
        return shopService.purchaseSkin(currentUserId(), skinId);
    }

    @GetMapping("/skins")
    public List<SkinDTO> listSkins() {
        return shopService.listSkins(currentUserId());
    }

    @GetMapping("/inventory/skins")
    public List<SkinDTO> listSkinInventory() {
        return shopService.listSkinInventory(currentUserId());
    }

    @PatchMapping("/skins/{skinId}/activate")
    public SkinDTO activateSkin(@PathVariable Long skinId) {
        return shopService.activateSkin(currentUserId(), skinId);
    }
}
