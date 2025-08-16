package com.example.hackathon.service;

import com.example.hackathon.dto.auth.LoginRequest;
import com.example.hackathon.dto.auth.LoginResponse;
import com.example.hackathon.dto.auth.SignUpRequest;
import com.example.hackathon.entity.User;
import com.example.hackathon.repository.UserRepository;
import com.example.hackathon.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Integer register(SignUpRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        String hash = passwordEncoder.encode(req.password());

        User saved = userRepository.save(
            User.builder()
                .nickname(req.nickname())
                .email(req.email())
                .passwordHash(hash)
                .birthDate(req.birthDate())
                .sido(req.sido())
                .sigungu(req.sigungu())
                .dong(req.dong())
                .role(req.role())
                    .pref1(req.pref1())
                    .pref2(req.pref2())
                    .pref3(req.pref3())
                .locationConsent(req.locationConsent())
                .marketingConsent(req.marketingConsent())
                .serviceAgreed(req.serviceAgreed())
                .privacyAgreed(req.privacyAgreed())
                .build()
        );
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole());
        return new LoginResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                token,
                "Bearer",
                "로그인 성공"
        );
    }
}
