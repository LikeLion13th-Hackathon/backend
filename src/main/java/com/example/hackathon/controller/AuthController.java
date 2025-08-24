package com.example.hackathon.controller;

import com.example.hackathon.dto.auth.LoginRequest;
import com.example.hackathon.dto.auth.LoginResponse;
import com.example.hackathon.dto.auth.SignUpRequest;
import com.example.hackathon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest req) {
        Integer id = userService.register(req);
        return ResponseEntity.ok().body(
            java.util.Map.of("userId", id, "message", "회원가입 성공")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }

    // 토큰 테스트용 (선택)
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        // JwtAuthenticationFilter에서 SecurityContext에 인증이 올라가므로,
        // 실제 서비스에서는 @AuthenticationPrincipal 등을 활용해 유저 정보를 주로 반환.
        return ResponseEntity.ok(java.util.Map.of("message", "토큰 유효, 인증 성공"));
    }
}