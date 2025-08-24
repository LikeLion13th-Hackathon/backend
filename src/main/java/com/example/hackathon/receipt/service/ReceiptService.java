package com.example.hackathon.receipt.service;

import com.example.hackathon.receipt.entity.Receipt;
import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.receipt.OcrStatus;
import com.example.hackathon.receipt.VerificationStatus;
import com.example.hackathon.receipt.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptService {

    private final ReceiptRepository receiptRepository;

    /**
     * 업로드 루트 경로 (application.yml의 app.upload.base-dir)
     * 로컬 예: D:/Hackathon/backend/uploads/receipts
     * 배포 예: /home/ubuntu/uploads/receipts
     */
    @Value("${app.upload.base-dir:uploads/receipts}")
    private String baseDir;

    /** 배포 시 공개 URL 구성 요소 */
    @Value("${app.public.base-url:}")     // prod에선 https://your-domain.com
    private String publicBaseUrl;

    @Value("${app.public.path-prefix:/receipts}") // Nginx location 과 동일해야 함
    private String publicPathPrefix;

    // 서버 기동 시 실제 사용 경로 로그로 확인용
    @PostConstruct
    public void logBaseDir() {
        Path p = Paths.get(baseDir).toAbsolutePath().normalize();
        System.out.println("[UPLOAD BASE DIR] " + p);
        System.out.println("[PUBLIC BASE URL] " + publicBaseUrl);
        System.out.println("[PUBLIC PATH] " + publicPathPrefix);
    }

    /** 업로드 + Receipt(PENDING) 생성 */
    public Receipt uploadAndCreatePending(User user, UserMission userMission, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // baseDir을 절대경로로 강제
        Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();

        // 날짜 폴더 (예: 2025-08-20)
        String ymd = LocalDate.now().toString();
        Path dir = basePath.resolve(ymd);

        // 상위 폴더까지 모두 생성
        Files.createDirectories(dir);

        // 파일명/확장자
        String orig = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "receipt";
        String ext = "";
        int dot = orig.lastIndexOf('.');
        if (dot > -1) ext = orig.substring(dot);

        String savedName = UUID.randomUUID() + ext;
        Path savePath = dir.resolve(savedName);

        // Tomcat 임시경로 이슈 방지: Path 오버로드 사용
        file.transferTo(savePath);

        // 상대경로 계산 (예: "2025-08-20/uuid.jpg")
        String relative = basePath.relativize(savePath).toString().replace("\\", "/");

        // 배포 환경(prod)에서만 publicUrl 생성 (publicBaseUrl 설정되어 있을 때)
        String publicUrl = (StringUtils.hasText(publicBaseUrl))
                ? String.format("%s%s/%s", publicBaseUrl, publicPathPrefix, relative)
                : null; // 로컬(dev)에서는 null 유지

        // 엔티티 저장
        Receipt receipt = Receipt.builder()
                .user(user)
                .userMission(userMission)
                .originalFilename(orig)
                .storagePath(savePath.toAbsolutePath().toString().replace("\\", "/")) // 내부용
                .publicUrl(publicUrl)  // 배포면 실제 URL, 로컬이면 null
                .ocrStatus(OcrStatus.PENDING)
                // 스냅샷 & 초기 검증상태
                .missionPlaceCategory(userMission.getPlaceCategory())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        return receiptRepository.save(receipt);
    }

    // 진행 중인 미션의 영수증 목록 조회 (최신순)
    @Transactional(readOnly = true)
    public List<Receipt> findAllByUserMission(Long userMissionId) {
        return receiptRepository.findByUserMission_IdOrderByIdDesc(userMissionId);
    }

    /**
     * 특정 유저 소유의 Receipt 단건 조회
     * - 리포지토리 시그니처(Long/Integer) 타입 문제 회피를 위해 id로 조회 후 소유자 검사
     */
    @Transactional(readOnly = true)
    public Receipt findOwnedReceipt(Long id, Integer userId) {
        return receiptRepository.findById(id)
                .filter(r -> r.getUser() != null && r.getUser().getId() != null
                        && r.getUser().getId().equals(userId))
                .orElse(null);
    }

    // 파일 Content-Type 추정
    @Transactional(readOnly = true)
    public MediaType probeMediaType(Path path) throws IOException {
        String contentType = Files.probeContentType(path);
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    // 파일 리소스 열기
    @Transactional(readOnly = true)
    public InputStreamResource openFileResource(Path path) throws IOException {
        return new InputStreamResource(Files.newInputStream(path));
    }

    // ===================== OCR 후처리(분류/검증) =====================

    /**
     * OCR 성공 시 호출해서 결과 저장 + 카테고리 분류 + 검증까지 처리
     * 컨트롤러/리시버에서 OCR 파서 결과를 받아 이 메서드에 전달해줘.
     */
    public void handleOcrSucceeded(
            Long receiptId,
            String storeName,
            Integer amount,
            LocalDateTime purchaseAt,
            String ocrRawJson
    ) {
        Receipt r = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("receipt not found: " + receiptId));

        // 1) OCR 기본 필드 업데이트
        r.setOcrStatus(OcrStatus.SUCCEEDED);
        r.setStoreName(storeName);
        r.setAmount(amount);
        r.setPurchaseAt(purchaseAt);
        r.setOcrRawJson(ocrRawJson);

        // 2) 간단 룰/매핑으로 카테고리 판별
        ClassifyResult cr = classifyCategory(storeName, ocrRawJson);
        r.setDetectedPlaceCategory(cr.category);
        r.setDetectedConfidence(cr.confidence);

        // 3) 업로드 당시 스냅샷과의 매칭 검증
        if (cr.category == null) {
            r.setVerificationStatus(VerificationStatus.REJECTED);
            r.setRejectReason("카테고리 분류 실패");
            return;
        }

        PlaceCategory required = r.getMissionPlaceCategory(); // 업로드 시 스냅샷
        if (required != null && required != cr.category) {
            r.setVerificationStatus(VerificationStatus.REJECTED);
            r.setRejectReason("미션 요구 카테고리 불일치");
            return;
        }

        // (선택) 금액/시간 검증 규칙이 있으면 추가
        // ex) if (minAmount != null && (amount == null || amount < minAmount)) { ... }

        r.setVerificationStatus(VerificationStatus.MATCHED);
        r.setRejectReason(null);
    }

    /** OCR 실패 처리(로그/모니터링은 별도) */
    public void handleOcrFailed(Long receiptId, String reason) {
        Receipt r = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("receipt not found: " + receiptId));

        r.setOcrStatus(OcrStatus.FAILED);
        r.setVerificationStatus(VerificationStatus.REJECTED);
        r.setRejectReason(
                StringUtils.hasText(reason) ? trimToLen(reason, 250) : "OCR 실패"
        );
    }

    // ===================== 간단 카테고리 분류기 =====================

    private ClassifyResult classifyCategory(String storeName, String rawJson) {
        String s = normalize(storeName);

        // 여기에 "예상되는 상호명 키워드"를 계속 추가해두면 됨
        // 카페
        if (anyMatch(s, "카페", "cafe", "스타벅", "투썸", "파스쿠찌", "엔제리너스", "메가커피", "빽다방", "할리스", "원어나더"))
            return new ClassifyResult(PlaceCategory.CAFE, 85);

        // 도서관
        if (anyMatch(s, "도서관", "library"))
            return new ClassifyResult(PlaceCategory.LIBRARY, 90);

        // 전통시장
        if (anyMatch(s, "시장", "전통시장"))
            return new ClassifyResult(PlaceCategory.TRADITIONAL_MARKET, 80);

        // 식당
        if (anyMatch(s, "식당", "restaurant", "한돈당", "국밥", "김밥", "분식", "족발", "치킨", "피자", "버거", "포차"))
            return new ClassifyResult(PlaceCategory.RESTAURANT, 75);

        // 쇼핑몰/상가(필요시)
        if (anyMatch(s, "몰", "shopping mall", "아울렛", "백화점", "롯데쇼핑(주)"))
            return new ClassifyResult(PlaceCategory.SHOPPING_MALL, 70);

        // 공원/체육(필요시)
        if (anyMatch(s, "공원", "park"))
            return new ClassifyResult(PlaceCategory.PARK, 70);

        if (anyMatch(s, "헬스", "짐", "휘트니스", "체육"))
            return new ClassifyResult(PlaceCategory.SPORTS_FACILITY, 70);

        // rawJson 안의 품목/키워드로 보조 판별 (예: 아메리카노, 라떼 등)
        if (StringUtils.hasText(rawJson)) {
            String rj = normalize(rawJson);
            if (rj.contains("아메리카노") || rj.contains("라떼") || rj.contains("에스프레소")) {
                return new ClassifyResult(PlaceCategory.CAFE, 65);
            }
        }

        // 모르면 null
        return new ClassifyResult(null, 0);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String out = s.toLowerCase().trim();
        // 필요시 특수문자 제거
        return out.replaceAll("[\\p{Punct}\\p{IsWhite_Space}]+", " ");
    }

    private static boolean anyMatch(String target, String... keys) {
        if (!StringUtils.hasText(target)) return false;
        for (String k : keys) {
            if (StringUtils.hasText(k) && target.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    private static String trimToLen(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static class ClassifyResult {
        final PlaceCategory category;
        final int confidence;
        ClassifyResult(PlaceCategory category, int confidence) {
            this.category = category;
            this.confidence = confidence;
        }
    }
}
