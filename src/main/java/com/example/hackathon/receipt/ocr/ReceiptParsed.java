package com.example.hackathon.receipt.ocr;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptParsed(
        String storeName,          // 업체명
        String storeAddressFull,   // 업체 전체 주소
        String storeAddressSiDo,   // 시/도
        String storeAddressGuGun,  // 구/군
        String storeAddressDong,   // 동
        LocalDateTime paidAt,      // 결제 일시
        BigDecimal totalAmount     // 합계 금액
) {}
