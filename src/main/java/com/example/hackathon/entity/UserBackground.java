package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(
  name = "user_backgrounds",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_user_background",
    columnNames = {"user_id","background_id"}
  )
)
@Getter @Setter
@NoArgsConstructor
public class UserBackground {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_background_id")
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Integer userId;          // FK -> user.id (INT)

  @Column(name = "background_id", nullable = false)
  private Long backgroundId;       // FK -> backgrounds.background_id (BIGINT)
}
