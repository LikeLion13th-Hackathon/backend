package com.example.hackathon.receipt.dto;

import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.receipt.OcrStatus;
import com.example.hackathon.receipt.VerificationStatus;
import com.example.hackathon.receipt.entity.Receipt;

import java.time.LocalDateTime;

public record ReceiptResponse(
        Long receiptId,
        Long missionId,

        // 상태
        OcrStatus ocrStatus,
        VerificationStatus verificationStatus,
        String rejectReason,

        // 파일 정보 (노출용 URL 제거)
        String originalFilename,

        // OCR 결과
        String storeName,
        LocalDateTime purchaseAt,
        Integer amount,

        // 주소
        String storeAddressFull,
        String storeAddressSiDo,
        String storeAddressGuGun,
        String storeAddressDong,

        // 카테고리(미션 요구/탐지 결과)
        PlaceCategory missionPlaceCategory,
        PlaceCategory detectedPlaceCategory,

        LocalDateTime createdAt
) {
    public static ReceiptResponse from(Receipt r) {
        return new ReceiptResponse(
                r.getId(),
                r.getUserMission().getId(),
                r.getOcrStatus(),
                r.getVerificationStatus(),
                r.getRejectReason(),
                r.getOriginalFilename(),
                r.getStoreName(),
                r.getPurchaseAt(),
                r.getAmount(),
                r.getStoreAddressFull(),
                r.getStoreAddressSiDo(),
                r.getStoreAddressGuGun(),
                r.getStoreAddressDong(),
                r.getMissionPlaceCategory(),
                r.getDetectedPlaceCategory(),
                r.getCreatedAt()
        );
    }
}
