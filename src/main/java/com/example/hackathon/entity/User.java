package com.example.hackathon.entity;

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

    @Column(name="is_over_14", nullable=false)
    private Boolean isOver14;

    @Column(length=10)
    private String region;

    @Column(name="marketing_consent", nullable=false)
    private Boolean marketingConsent;

    @Column(name="service_agreed", nullable=false)
    private Boolean serviceAgreed;

    @Column(name="privacy_agreed", nullable=false)
    private Boolean privacyAgreed;
}
