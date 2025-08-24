// entity/UserCharacterProgress.java
package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// src/main/java/com/example/hackathon/entity/UserCharacterProgress.java
@Entity
@Table(name = "user_character_progress")
@Getter @Setter @NoArgsConstructor
public class UserCharacterProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "skin_id", nullable = false)
    private Long skinId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "feed_progress", nullable = false)
    private Integer feedProgress = 0;

    // ★ 추가: 스킨별 표시 이름
    @Column(name = "display_name", length = 20)
    private String displayName;
}
