package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_skins")
@Getter @Setter @NoArgsConstructor
public class UserSkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_skin_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;          // FK: user.id (INT)

    @Column(name = "skin_id", nullable = false)
    private Long skinId;             // FK: character_skins.skin_id (BIGINT)

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt = LocalDateTime.now();
}
