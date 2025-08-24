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

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(nullable = false)
  private Integer level = 1;

  @Column(name = "feed_progress", nullable = false)
  private Integer feedProgress = 0;

  @Column(name = "active_background_id")
  private Long activeBackgroundId;

  @Column(name = "active_skin_id")
  private Long activeSkinId;

  // 캐릭터 종류 (기본: 삐약이)
  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false)
  private CharacterKind kind = CharacterKind.CHICK;

  // 유저가 설정하는 캐릭터 이름 (기본: 삐약이)
  @Column(name = "display_name", nullable = false)
  private String displayName = "삐약이";
}