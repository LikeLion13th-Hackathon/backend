package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkinDTO {
    private Long skinId;
    private String name;
    private int priceCoins;
    private boolean owned;
    private boolean active;
}
