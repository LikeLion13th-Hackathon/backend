package com.example.hackathon.receipt.dto;

import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.receipt.OcrStatus;
import com.example.hackathon.receipt.VerificationStatus;
import com.example.hackathon.receipt.entity.Receipt;

import java.time.LocalDateTime;

public record ReceiptResponse(
        Long receiptId,
        Long missionId,
        OcrStatus ocrStatus,
        VerificationStatus verificationStatus,
        String rejectReason,
        String originalFilename,
        String storeName,
        LocalDateTime purchaseAt,
        Integer amount,
        String storeAddressFull,
        String storeAddressSiDo,
        String storeAddressGuGun,
        String storeAddressDong,
        PlaceCategory missionPlaceCategory,
        PlaceCategory detectedPlaceCategory,
        LocalDateTime createdAt
) {
    public static ReceiptResponse from(Receipt r) {
        return new ReceiptResponse(
                r.getId(),
                (r.getUserMission() != null ? r.getUserMission().getId() : null),
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
