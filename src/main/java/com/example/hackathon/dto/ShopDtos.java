package com.example.hackathon.dto;

import java.util.List;

public class ShopDtos {
    public record CharacterInfoDTO(int level, int feedProgress, int feedsRequiredToNext, Long activeBackgroundId) {}
    public record BackgroundDTO(long backgroundId, String name, int priceCoins, boolean owned, boolean active) {}
    public record ShopOverviewDTO(CharacterInfoDTO character, List<BackgroundDTO> backgrounds) {}
}
