package com.example.hackathon.receipt.service;

import com.example.hackathon.receipt.OcrStatus;
import com.example.hackathon.receipt.entity.Receipt;
import com.example.hackathon.receipt.ocr.ReceiptOcrParser;
import com.example.hackathon.receipt.ocr.ReceiptParsed;
import com.example.hackathon.receipt.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptOcrService {

    private final ReceiptRepository receiptRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // 분류/검증 로직 호출용
    private final ReceiptService receiptService;

    @Value("${clova.ocr.url}")
    private String clovaOcrUrl;

    @Value("${clova.ocr.secret}")
    private String clovaOcrSecret;


    public Receipt runOcr(Long receiptId) throws Exception {
        Receipt r = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("receipt not found: " + receiptId));

        r.setOcrStatus(OcrStatus.RUNNING);

        try {
            // 1) 파일 → base64
            Path local = Path.of(r.getStoragePath());
            byte[] bytes = Files.readAllBytes(local);
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);

            // 2) CLOVA OCR 요청 JSON
            String requestJson = """
            {
              "version": "V2",
              "requestId": "sample-id",
              "timestamp": %d,
              "images": [
                { "name": "receipt", "format": "jpg", "data": "%s" }
              ]
            }
            """.formatted(System.currentTimeMillis(), base64);

            // 3) 헤더
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", clovaOcrSecret);

            // 4) 호출
            ResponseEntity<String> resp = restTemplate.exchange(
                    clovaOcrUrl, HttpMethod.POST, new HttpEntity<>(requestJson, headers), String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("CLOVA OCR call failed: " + resp.getStatusCode());
            }

            String body = resp.getBody();

            // 5) JSON 파싱
            ReceiptParsed parsed = ReceiptOcrParser.parse(body);

            // 6) 분류/검증까지 한 번에 처리
            receiptService.handleOcrSucceeded(
                    receiptId,
                    parsed.storeName(),
                    parsed.totalAmount() != null ? parsed.totalAmount().intValue() : null,
                    parsed.paidAt(),
                    body // raw json 저장
            );

            // 7) 갱신된 엔티티로 반환
            return receiptRepository.findById(receiptId).orElseThrow();

        } catch (Exception e) {
            // (선택) 실패 마킹
            receiptService.handleOcrFailed(receiptId, e.getMessage());
            throw e;
        }
    }
}
