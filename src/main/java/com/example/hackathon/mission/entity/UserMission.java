package com.example.hackathon.mission.entity;

import com.example.hackathon.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "user_mission",
        indexes = {
                @Index(name="idx_user_mission_user", columnList = "user_id"),
                @Index(name="idx_user_mission_cat",  columnList = "category")
        })
public class UserMission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private MissionCategory category; // CUSTOM

    @Enumerated(EnumType.STRING)
    @Column(name="place_category", nullable=false, length=40)
    private PlaceCategory placeCategory;

    @Column(nullable=false, length=200)
    private String title;

    @Column(nullable=false, length=300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private VerificationType verificationType;

    @Column(name="min_amount")
    private Integer minAmount; // 금액 기준(원)

    @Column(name="reward_point", nullable=false)
    private Integer rewardPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private MissionStatus status;

    // 미션 유효 기간
    @Column(name="start_date", nullable=false)
    private LocalDate startDate;
    @Column(name="end_date", nullable=false)
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    // 진행 타임라인
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /**
     * 포기 시 "처음 상태"로 되돌리기 위한 초기화 메서드
     * - 상태를 READY 로 변경
     * - 타임라인 필드 초기화
     * 필요 시 검증/진행 관련 추가 필드가 있다면 여기서 함께 초기화
     */
    public void resetToReady() {
        this.status = MissionStatus.READY;
        this.startedAt = null;
        this.completedAt = null;
    }
}
