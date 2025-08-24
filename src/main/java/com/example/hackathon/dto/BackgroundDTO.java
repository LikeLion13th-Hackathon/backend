// src/main/java/com/example/hackathon/dto/BackgroundDTO.java
package com.example.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null이면 키 자체를 숨기고 싶다면 유지
public class BackgroundDTO {
  private long backgroundId;
  private String name;
  private int priceCoins;
  private boolean owned;
  private boolean active;
  private Integer balance; // ★ 추가: 구매 후 잔액 (조회/목록에선 null)

  // 조회/목록용(잔액 없이) 생성자
  public BackgroundDTO(long backgroundId, String name, int priceCoins, boolean owned, boolean active) {
    this(backgroundId, name, priceCoins, owned, active, null);
  }
}
