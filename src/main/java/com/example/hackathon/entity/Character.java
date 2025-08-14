package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "characters")
@Getter @Setter
@NoArgsConstructor
public class Character {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "character_id")
  private Long id;

  // user.id = INT 이므로 Integer 사용
  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(nullable = false)
  private Integer level = 1;

  @Column(name = "feed_progress", nullable = false)
  private Integer feedProgress = 0;

  // backgrounds.background_id = BIGINT
  @Column(name = "active_background_id")
  private Long activeBackgroundId;
}