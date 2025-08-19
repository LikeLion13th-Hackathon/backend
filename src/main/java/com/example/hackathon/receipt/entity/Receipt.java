package com.example.hackathon.receipt.entity;

import com.example.hackathon.entity.User;                    // ✅ 네 프로젝트의 User
import com.example.hackathon.mission.entity.UserMission;     // ✅ 네 프로젝트의 UserMission
import com.example.hackathon.receipt.OcrStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 영수증 업로드 기록 + OCR 결과 저장
 * FK:
 * - user_id         -> user.id
 * - user_mission_id -> user_mission.id
 */
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

    /** 업로드한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 사용자의 미션 진행건 (한 진행건에 여러 장 올릴 수 있음) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_mission_id", nullable = false)
    private UserMission userMission;

    /** 원본 파일명(표시용, 신뢰 X) */
    @Column(nullable = false, length = 255)
    private String originalFilename;

    /** 서버 내부 저장 경로(백엔드용) — 클라이언트에 절대 노출하지 않음 */
    @Column(nullable = false, length = 512)
    private String storagePath;

    /** 브라우저에서 바로 열 수 있는 URL (정적서빙/S3 쓰면 채움; 지금은 null 가능) */
    @Column(length = 512)
    private String publicUrl;

    /** OCR 처리 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "ocr_status", nullable = false, length = 16)
    private OcrStatus ocrStatus;

    // ---- OCR 결과(후속 단계에서 채움) ----
    private String storeName;                 // 가게명
    private Integer amount;                   // 결제금액(원)
    private LocalDateTime purchaseAt;         // 결제시각

    @Lob
    private String ocrRawJson;                // OCR 원문(JSON 문자열)

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
