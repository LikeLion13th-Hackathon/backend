// src/main/java/com/example/hackathon/receipt/service/ReceiptService.java
package com.example.hackathon.receipt.service;

import com.example.hackathon.receipt.entity.Receipt;
import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.receipt.OcrStatus;
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
import java.util.List;
import java.util.UUID;

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

    /** 서버 기동 시 실제 사용 경로 로그로 확인용 (선택) */
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

        // ✅ baseDir을 절대경로로 강제
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

        // 🚨 Tomcat 임시경로 이슈 방지: Path 오버로드 사용
        file.transferTo(savePath);

        // 👉 상대경로 계산 (예: "2025-08-20/uuid.jpg")
        String relative = basePath.relativize(savePath).toString().replace("\\", "/");

        // 👉 배포 환경(prod)에서만 publicUrl 생성 (publicBaseUrl 설정되어 있을 때)
        String publicUrl = (StringUtils.hasText(publicBaseUrl))
                ? String.format("%s%s/%s", publicBaseUrl, publicPathPrefix, relative)
                : null; // 로컬(dev)에서는 null 유지

        // 엔티티 저장
        Receipt receipt = Receipt.builder()
                .user(user)
                .userMission(userMission)
                .originalFilename(orig)
                .storagePath(savePath.toAbsolutePath().toString().replace("\\", "/")) // 내부용
                .publicUrl(publicUrl)  // ✅ 배포면 실제 URL, 로컬이면 null
                .ocrStatus(OcrStatus.PENDING)
                .build();

        return receiptRepository.save(receipt);
    }

    /** 진행 중인 미션의 영수증 목록 조회 (최신순) */
    @Transactional(readOnly = true)
    public List<Receipt> findAllByUserMission(Long userMissionId) {
        return receiptRepository.findByUserMission_IdOrderByIdDesc(userMissionId);
    }

    /** 특정 유저 소유의 Receipt 단건 조회 */
    @Transactional(readOnly = true)
    public Receipt findOwnedReceipt(Long id, Integer userId) {
        return receiptRepository.findByIdAndUser_Id(id, userId).orElse(null);
    }

    /** 파일 Content-Type 추정 */
    @Transactional(readOnly = true)
    public MediaType probeMediaType(Path path) throws IOException {
        String contentType = Files.probeContentType(path);
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    /** 파일 리소스 열기 */
    @Transactional(readOnly = true)
    public InputStreamResource openFileResource(Path path) throws IOException {
        return new InputStreamResource(Files.newInputStream(path));
    }
}
