package com.example.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true) // ← 예상치 못한 필드 들어와도 무시
public class SkinDTO {
    private Long skinId;
    private String name;
    private int priceCoins;
    private boolean owned;
    private boolean active;
    private Integer balance; // 목록에선 null → 미출력, 구매 응답에선 값 채움

    // 조회/목록용(잔액 없이) 생성자
    public SkinDTO(Long skinId, String name, int priceCoins, boolean owned, boolean active) {
        this(skinId, name, priceCoins, owned, active, null);
    }
}
