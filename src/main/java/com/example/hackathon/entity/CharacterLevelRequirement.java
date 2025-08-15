package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "character_level_requirements")
@Getter @Setter
@NoArgsConstructor
public class CharacterLevelRequirement {

  @Id
  @Column(name = "level")
  private Integer level;

  @Column(name = "feeds_required", nullable = false)
  private Integer feedsRequired;
}
