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
}
