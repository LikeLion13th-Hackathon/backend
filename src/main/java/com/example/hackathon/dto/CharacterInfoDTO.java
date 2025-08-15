package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CharacterInfoDTO {
  private int level;
  private int feedProgress;
  private int feedsRequiredToNext;
  private Long activeBackgroundId;
}
