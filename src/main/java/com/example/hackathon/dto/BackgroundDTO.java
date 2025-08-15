package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BackgroundDTO {
  private long backgroundId;
  private String name;
  private int priceCoins;
  private boolean owned;
  private boolean active;
}
