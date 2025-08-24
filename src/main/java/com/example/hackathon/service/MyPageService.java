// src/main/java/com/example/hackathon/mypage/service/MyPageService.java
package com.example.hackathon.mypage.service;

import com.example.hackathon.dto.mypage.MyPageResponseDTO;
import com.example.hackathon.dto.mypage.MyPageUpdateDTO;
import com.example.hackathon.entity.User;
import com.example.hackathon.mission.dto.MissionResponse;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;

    @PersistenceContext
    private final EntityManager em;

    // ===== 상단: 내 정보 조회 =====
    @Transactional(readOnly = true)
    public MyPageResponseDTO getMyInfo(Long userId) {
        User u = getUser(userId);

        // pref1~3 → Set<String>
        Set<String> preferPlaces = new LinkedHashSet<>();
        if (u.getPref1() != null) preferPlaces.add(u.getPref1().name());
        if (u.getPref2() != null) preferPlaces.add(u.getPref2().name());
        if (u.getPref3() != null) preferPlaces.add(u.getPref3().name());

        return MyPageResponseDTO.builder()
                .nickname(u.getNickname())
                .email(u.getEmail())
                .birthDate(u.getBirthDate())
                .job(u.getRole())
                .regionSido(u.getSido())
                .regionGungu(u.getSigungu())
                .regionDong(u.getDong())
                .preferPlaces(preferPlaces)
                .build();
    }

    // ===== 상단: 내 정보 수정 (PATCH) =====
    @Transactional
    public MyPageResponseDTO updateMyInfo(Long userId, MyPageUpdateDTO dto) {
        User u = getUser(userId);

        if (dto.getNickname() != null) u.setNickname(dto.getNickname());
        if (dto.getBirthDate() != null && !dto.getBirthDate().isBlank()) {
            u.setBirthDate(LocalDate.parse(dto.getBirthDate()));
        }
        if (dto.getJob() != null) {
            u.setRole(dto.getJob());
        }
        if (dto.getRegionSido()  != null) u.setSido(dto.getRegionSido());
        if (dto.getRegionGungu() != null) u.setSigungu(dto.getRegionGungu());
        if (dto.getRegionDong()  != null) u.setDong(dto.getRegionDong());

        if (dto.getPreferPlaces() != null) {
            List<PlaceCategory> parsed = dto.getPreferPlaces().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try { return PlaceCategory.valueOf(s); }
                        catch (IllegalArgumentException e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(3)
                    .collect(Collectors.toList());

            if (!parsed.isEmpty()) {
                u.setPref1(parsed.size() > 0 ? parsed.get(0) : u.getPref1());
                u.setPref2(parsed.size() > 1 ? parsed.get(1) : null);
                u.setPref3(parsed.size() > 2 ? parsed.get(2) : null);
            }
        }

        return getMyInfo(userId);
    }

    // ===== 진행/완료 목록 =====
    @Transactional(readOnly = true)
    public List<MissionResponse> getInProgressMissions(Long userId) {
        return extractByStatus(getUser(userId), MissionStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> getCompletedMissions(Long userId) {
        return extractByStatus(getUser(userId), MissionStatus.COMPLETED);
    }

    // ===== 내부 헬퍼 =====
    private User getUser(Long userId) {
        return userRepository.findById(userId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다. id=" + userId));
    }

    private List<MissionResponse> extractByStatus(User user, MissionStatus status) {
        List<UserMission> list = em.createQuery("""
                select um
                  from UserMission um
                 where um.user = :user
                   and um.status = :status
                 order by um.createdAt asc
                """, UserMission.class)
                .setParameter("user", user)
                .setParameter("status", status)
                .getResultList();

        return MissionResponse.fromList(list); // ✅ 공용 변환기
    }
}
