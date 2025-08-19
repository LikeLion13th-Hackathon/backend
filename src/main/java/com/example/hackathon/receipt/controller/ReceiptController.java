package com.example.hackathon.receipt.controller;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.repository.UserMissionRepository;
import com.example.hackathon.receipt.dto.ReceiptResponse;
import com.example.hackathon.receipt.entity.Receipt;
import com.example.hackathon.receipt.service.ReceiptService;
import com.example.hackathon.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/receipt/{mission_id}") // mission_id = 진행중 미션(user_mission) id
public class ReceiptController {

    private final ReceiptService receiptService;
    private final UserRepository userRepository;
    private final UserMissionRepository userMissionRepository;


    private String resolveEmail(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && StringUtils.hasText(auth.getName())) {
            return auth.getName();
        }
        return null;
    }


    // [업로드] 영수증 이미지 업로드 + Receipt(PENDING) 생성
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ReceiptResponse> upload(
            @PathVariable("mission_id") Long missionId,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) throws Exception {

        String email = resolveEmail(request);
        if (!StringUtils.hasText(email)) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        UserMission um = userMissionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 진행 미션입니다."));

        // 소유권 검증
        if (!um.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        Receipt saved = receiptService.uploadAndCreatePending(user, um, file);
        return ResponseEntity.ok(ReceiptResponse.from(saved));
    }


    // 해당 진행 미션의 영수증 목록(최신순)
    @GetMapping
    public ResponseEntity<List<ReceiptResponse>> list(
            @PathVariable("mission_id") Long missionId,
            HttpServletRequest request
    ) {
        String email = resolveEmail(request);
        if (!StringUtils.hasText(email)) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        UserMission um = userMissionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 진행 미션입니다."));
        if (!um.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        List<Receipt> receipts = receiptService.findAllByUserMission(missionId);
        List<ReceiptResponse> body = receipts.stream()
                .map(ReceiptResponse::from)
                .toList();

        return ResponseEntity.ok(body);
    }


    // 영수증 단건 조회
    @GetMapping("/{receipt_id}")
    public ResponseEntity<ReceiptResponse> getOne(
            @PathVariable("mission_id") Long missionId,
            @PathVariable("receipt_id") Long receiptId,
            HttpServletRequest request
    ) {
        String email = resolveEmail(request);
        if (!StringUtils.hasText(email)) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Receipt receipt = receiptService.findOwnedReceipt(receiptId, user.getId());
        if (receipt == null) return ResponseEntity.status(404).build();
        if (!receipt.getUserMission().getId().equals(missionId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(ReceiptResponse.from(receipt));
    }


    // 영수증 이미지 불러오기
    @GetMapping("/{receipt_id}/file")
    public ResponseEntity<?> download(
            @PathVariable("mission_id") Long missionId,
            @PathVariable("receipt_id") Long receiptId,
            HttpServletRequest request
    ) throws Exception {

        String email = resolveEmail(request);
        if (!StringUtils.hasText(email)) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Receipt receipt = receiptService.findOwnedReceipt(receiptId, user.getId());
        if (receipt == null) return ResponseEntity.status(404).build();
        if (!receipt.getUserMission().getId().equals(missionId)) {
            return ResponseEntity.status(403).build();
        }

        Path path = Paths.get(receipt.getStoragePath());
        if (!Files.exists(path)) {
            return ResponseEntity.status(404).body("파일이 없습니다.");
        }

        var mediaType = receiptService.probeMediaType(path);
        InputStreamResource resource = receiptService.openFileResource(path);

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + receipt.getOriginalFilename() + "\"")
                .header("Cache-Control", "no-store")
                .contentType(mediaType)
                .body(resource);
    }
}
