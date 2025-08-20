package com.example.hackathon.receipt.entity;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.receipt.OcrStatus;
import com.example.hackathon.receipt.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "receipt",
        indexes = {
                @Index(name = "idx_receipt_user", columnList = "user_id"),
                @Index(name = "idx_receipt_user_mission", columnList = "user_mission_id"),
                @Index(name = "idx_receipt_status", columnList = "ocr_status")
        })
public class Receipt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 업로드한 사용자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 사용자의 미션 진행건 (한 진행건에 여러 장 올릴 수 있음)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_mission_id", nullable = false)
    private UserMission userMission;

    // 원본 파일명(표시용, 신뢰 X)
    @Column(nullable = false, length = 255)
    private String originalFilename;

    // 서버 내부 저장 경로(백엔드용)
    @Column(nullable = false, length = 512)
    private String storagePath;

    // 브라우저에서 바로 열 수 있는 URL
    @Column(length = 512)
    private String publicUrl;

    // OCR 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "ocr_status", nullable = false, length = 16)
    private OcrStatus ocrStatus;

    // OCR 결과
    private String storeName;            // 가게명
    private Integer amount;              // 결제금액(원)
    private LocalDateTime purchaseAt;    // 결제시각

    @Lob
    private String ocrRawJson;           // OCR 원문(JSON 문자열)


    // 미션 요구 카테고리 스냅샷 (업로드 시 userMission.placeCategory를 복사)
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_place_category", length = 32)
    private PlaceCategory missionPlaceCategory;

    // OCR/룰/수동으로 판별된 실제 카테고리
    @Enumerated(EnumType.STRING)
    @Column(name = "detected_place_category", length = 32)
    private PlaceCategory detectedPlaceCategory;

    // 분류 신뢰도 (0~100) -> 백엔드 확인용으로만 둠
    @Column(name = "detected_confidence")
    private Integer detectedConfidence;

    // 영수증 검증 상태: PENDING/MATCHED/REJECTED
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 16)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    // 검증 실패 사유(있을 때만)
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
