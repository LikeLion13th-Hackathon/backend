package com.example.hackathon.receipt.ocr;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptParsed(
        String storeName, // 업체명
        String storeAddress, // 업체 주소
        LocalDateTime paidAt, // 결제 일시
        BigDecimal totalAmount // 합계 금액
) {}
