package com.example.hackathon.mission.dto;

import com.example.hackathon.mission.entity.VerificationType;
import lombok.Data;

// 검증용도 개선 예정

@Data
public class CompleteRequest {
    private VerificationType verificationType; // 기본 RECEIPT_OCR
}
