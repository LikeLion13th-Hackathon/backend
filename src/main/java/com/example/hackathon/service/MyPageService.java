package com.example.hackathon.service;

import com.example.hackathon.dto.mypage.MyPageResponseDTO;
import com.example.hackathon.dto.mypage.MyPageUpdateDTO;
import com.example.hackathon.entity.User;
import com.example.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;

    // 이메일 기반 조회
    public MyPageResponseDTO getMyPageByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        return MyPageResponseDTO.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .job(user.getRole())
                .isOver14(true) // DB에 필드 없으므로 기본 true
                .marketingConsent(user.getMarketingConsent())
                .serviceAgreed(user.getServiceAgreed())
                .privacyAgreed(user.getPrivacyAgreed())
                .locationConsent(user.getLocationConsent())
                .build();
    }

    // 이메일 기반 수정
    public void updateMyPageByEmail(String email, MyPageUpdateDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getJob() != null) user.setRole(dto.getJob());
        if (dto.getMarketingConsent() != null) user.setMarketingConsent(dto.getMarketingConsent());
        if (dto.getLocationConsent() != null) user.setLocationConsent(dto.getLocationConsent());

        userRepository.save(user);
    }
}
