// src/main/java/com/example/hackathon/dto/CharacterInfoDTO.java
package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CharacterInfoDTO {
  private Long characterId;        // ★ 추가: 활성 캐릭터 PK
  private int level;
  private int feedProgress;
  private int feedsRequiredToNext;
  private Long activeBackgroundId;
  private String title;            // ★ 추가: 레벨별 타이틀
  private String displayName;      // ★ 추가: 유저가 설정한 캐릭터 이름
}
