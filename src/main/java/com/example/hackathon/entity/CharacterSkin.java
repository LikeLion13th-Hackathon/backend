package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "character_skins")
@Getter @Setter @NoArgsConstructor
public class CharacterSkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skin_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_coins", nullable = false)
    private Integer priceCoins;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "image_url")
    private String imageUrl; // 선택
}
