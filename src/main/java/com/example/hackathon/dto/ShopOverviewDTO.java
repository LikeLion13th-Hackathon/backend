package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopOverviewDTO {

  private CharacterInfoDTO character;   // 캐릭터 정보 (id, name, level 등)
}
