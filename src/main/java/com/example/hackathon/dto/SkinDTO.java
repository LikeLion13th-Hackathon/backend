package com.example.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkinDTO {
    private Long skinId;
    private String name;
    private int priceCoins;
    private boolean owned;
    private boolean active;
    private Integer balance;

    // 조회/목록용(잔액 없이) 생성자
    public SkinDTO(Long skinId, String name, int priceCoins, boolean owned, boolean active) {
        this(skinId, name, priceCoins, owned, active, null);
    }
}
