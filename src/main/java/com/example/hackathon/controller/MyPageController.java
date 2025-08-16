package com.example.hackathon.controller;

import com.example.hackathon.dto.mypage.MyPageResponseDTO;
import com.example.hackathon.dto.mypage.MyPageUpdateDTO;
import com.example.hackathon.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    // 마이페이지 조회 (JWT subject = email)
    @GetMapping
    public ResponseEntity<MyPageResponseDTO> getMyPage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // JWT subject → email
        return ResponseEntity.ok(myPageService.getMyPageByEmail(email));
    }

    // 마이페이지 수정 (JWT subject = email)
    @PutMapping
    public ResponseEntity<String> updateMyPage(@RequestBody MyPageUpdateDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // JWT subject → email
        myPageService.updateMyPageByEmail(email, dto);
        return ResponseEntity.ok("마이페이지가 수정되었습니다.");
    }
}
