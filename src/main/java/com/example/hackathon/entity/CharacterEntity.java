package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "characters")
@Getter @Setter
@NoArgsConstructor
public class CharacterEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "character_id")
  private Long id;

  // user.id = INT
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
