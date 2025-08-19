package com.example.hackathon.receipt.dto;

import com.example.hackathon.receipt.entity.Receipt;
import com.example.hackathon.receipt.OcrStatus;

import java.time.LocalDateTime;


public record ReceiptResponse(
        Long receiptId,
        Long missionId,
        OcrStatus ocrStatus,
        String originalFilename,   // 표시용
        String publicUrl,          // 정적서빙/S3 쓰면 채움 (지금은 null 가능)
        LocalDateTime createdAt
) {
    public static ReceiptResponse from(Receipt r) {
        return new ReceiptResponse(
                r.getId(),
                r.getUserMission().getId(),
                r.getOcrStatus(),
                r.getOriginalFilename(),
                r.getPublicUrl(),
                r.getCreatedAt()
        );
    }
}
