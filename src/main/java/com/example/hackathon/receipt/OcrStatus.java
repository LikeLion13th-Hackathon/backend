package com.example.hackathon.receipt;

// 영수증 OCR 처리 상태
public enum OcrStatus {
    PENDING,   // 막 업로드된 상태
    RUNNING,   // OCR 중
    SUCCEEDED, // OCR 성공
    FAILED     // OCR 실패
}
