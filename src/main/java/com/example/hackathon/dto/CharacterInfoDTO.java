package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CharacterInfoDTO {
  private Long skinId;              // ✅ 변경: characterId 대신 skinId
  private int level;
  private int feedProgress;
  private int feedsRequiredToNext;
  private Long activeBackgroundId;
  private String title;            // 레벨별 타이틀
  private String displayName;      // 유저가 설정한 캐릭터 이름(닉네임)
}
