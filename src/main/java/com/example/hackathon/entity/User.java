// src/main/java/com/example/hackathon/entity/User.java
package com.example.hackathon.entity;

import com.example.hackathon.mission.entity.PlaceCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "`user`")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable=false, length=32)
    private String nickname;

    @Column(nullable=false, length=254, unique=true)
    private String email;

    @Column(name="password_hash", nullable=false, length=128)
    private String passwordHash;

    @Column(name="birth_date", nullable=false)
    private LocalDate birthDate;

    @Column(length=20)  private String sido;     // 시/도
    @Column(length=30)  private String sigungu;  // 시/군·구
    @Column(length=40)  private String dong;     // 읍·면·동
    @Column(length=20)  private String role;     // 역할(학생/주부/직장인 등)

    // 선호 장소 3개 (필수)
    @Enumerated(EnumType.STRING)
    @Column(name="pref1", nullable = false, length = 40)
    private PlaceCategory pref1;
    @Enumerated(EnumType.STRING)
    @Column(name="pref2", nullable = false, length = 40)
    private PlaceCategory pref2;
    @Enumerated(EnumType.STRING)
    @Column(name="pref3", nullable = false, length = 40)
    private PlaceCategory pref3;

    @Column(name="location_consent", nullable=false)
    private Boolean locationConsent;

    @Column(name="marketing_consent", nullable=false)
    private Boolean marketingConsent;

    @Column(name="service_agreed", nullable=false)
    private Boolean serviceAgreed;

    @Column(name="privacy_agreed", nullable=false)
    private Boolean privacyAgreed;
}
