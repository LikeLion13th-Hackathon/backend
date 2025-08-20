package com.example.hackathon.mission.dto;

import com.example.hackathon.mission.entity.VerificationType;
import lombok.Data;

@Data
public class CompleteRequest {
    private VerificationType verificationType; // 기본 RECEIPT_OCR
    private Long receiptId;                    // 영수증 기반 완료 시 사용
}
