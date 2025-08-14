package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "backgrounds")
@Getter @Setter
@NoArgsConstructor
public class Background {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "background_id")
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "price_coins", nullable = false)
  private Integer priceCoins;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;
}
