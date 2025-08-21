package com.example.hackathon.ai_custom.entity;

import com.example.hackathon.mission.entity.PlaceCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mission_template")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MissionTemplate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name="place_category", nullable=false, length=30)
    private PlaceCategory placeCategory;

    @Column(nullable=false, length=200)
    private String title;

    @Column(nullable=false, length=300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name="verification_type", nullable=false, length=20)
    private com.example.hackathon.mission.entity.VerificationType verificationType;

    @Column(name="min_amount")
    private Integer minAmount;

    @Column(name="reward_point", nullable=false)
    private Integer rewardPoint;

    @Column(nullable=false)
    private boolean active = true;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
