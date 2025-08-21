// package com.example.hackathon.mission.service;

// import com.example.hackathon.entity.User;
// import com.example.hackathon.mission.entity.MissionCategory;
// import com.example.hackathon.mission.entity.UserMission;
// import com.example.hackathon.mission.repository.UserMissionRepository;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.List;

// @Service
// @RequiredArgsConstructor
// @Transactional
// public class RegionMissionService {

//     private final UserMissionRepository userMissionRepository;

//     /**
//      * 특정 유저에게 지역 미션(맛집·명소·특산물) 더미데이터 생성
//      * - 이미 해당 유저에게 지역 미션이 있으면 생성하지 않음
//      */
//     public void initRegionMissions(User user) {
//         boolean exists = userMissionRepository.existsByUserAndCategoryIn(
//                 user,
//                 List.of(MissionCategory.RESTAURANT, MissionCategory.LANDMARK, MissionCategory.SPECIALTY)
//         );
//         if (exists) return;

//         // 지역 맛집 (RESTAURANT)
//         userMissionRepository.save(UserMission.builder()
//                 .user(user)
//                 .category(MissionCategory.RESTAURANT)
//                 .title("지역 맛집 탐방하기 1")
//                 .description("지역 맛집 첫 번째 방문 미션")
//                 .build());

//         userMissionRepository.save(UserMission.builder()
//                 .user(user)
//                 .category(MissionCategory.RESTAURANT)
//                 .title("지역 맛집 탐방하기 2")
//                 .description("지역 맛집 두 번째 방문 미션")
//                 .build());

//         // 지역 명소 (LANDMARK)
//         userMissionRepository.save(UserMission.builder()
//                 .user(user)
//                 .category(MissionCategory.LANDMARK)
//                 .title("지역 명소 방문하기 1")
//                 .description("지역 명소 첫 번째 탐방 미션")
//                 .build());

//         userMissionRepository.save(UserMission.builder()
//                 .user(user)
//                 .category(MissionCategory.LANDMARK)
//                 .title("지역 명소 방문하기 2")
//                 .description("지역 명소 두 번째 탐방 미션")
//                 .build());

//         // 특산물 (SPECIALTY)
//         userMissionRepository.save(UserMission.builder()
//                 .user(user)
//                 .category(MissionCategory.SPECIALTY)
//                 .title("지역 특산물 체험하기 1")
//                 .description("지역 특산물 첫 번째 체험 미션")
//                 .build());

//         userMissionRepository.save(UserMission.builder()
//                 .user(user)
//                 .category(MissionCategory.SPECIALTY)
//                 .title("지역 특산물 체험하기 2")
//                 .description("지역 특산물 두 번째 체험 미션")
//                 .build());
//     }

//     // 카테고리별 목록 조회
//     public List<UserMission> listByCategory(User user, MissionCategory category) {
//         return userMissionRepository.findMissionsByUserAndCategory(user, category);
//     }

//     // 카테고리별 단건 조회
//     public UserMission getOneByCategory(User user, Long id, MissionCategory category) {
//         return userMissionRepository.findMissionByIdAndUserAndCategory(id, user, category)
//                 .orElseThrow(() -> new IllegalArgumentException("미션이 없거나 권한이 없습니다. id=" + id));
//     }
// }

package com.example.hackathon.mission.service;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.entity.VerificationType;
import com.example.hackathon.mission.repository.UserMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RegionMissionService {

    private final UserMissionRepository userMissionRepository;

    /**
     * 특정 유저에게 지역 미션(맛집·명소·특산물) 더미데이터 생성
     * → 카테고리별로 2개씩 무조건 생성
     */
    public void initRegionMissions(User user) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusWeeks(3);

        // 🍜 지역 맛집 (RESTAURANT)
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.RESTAURANT)
                .placeCategory(PlaceCategory.RESTAURANT)
                .title("지역 맛집 탐방하기 1")
                .description("지역 맛집 첫 번째 방문 미션")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(5000)
                .verificationType(VerificationType.PHOTO)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.RESTAURANT)
                .placeCategory(PlaceCategory.RESTAURANT)
                .title("지역 맛집 탐방하기 2")
                .description("지역 맛집 두 번째 방문 미션")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(5000)
                .verificationType(VerificationType.PHOTO)
                .build());

        // 🏛 지역 명소 (LANDMARK)
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.LANDMARK)
                .placeCategory(PlaceCategory.MUSEUM)
                .title("지역 명소 방문하기 1")
                .description("지역 명소 첫 번째 탐방 미션")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(0)
                .verificationType(VerificationType.PHOTO)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.LANDMARK)
                .placeCategory(PlaceCategory.MUSEUM)
                .title("지역 명소 방문하기 2")
                .description("지역 명소 두 번째 탐방 미션")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(0)
                .verificationType(VerificationType.PHOTO)
                .build());

        // 🛍 특산물 (SPECIALTY)
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.SPECIALTY)
                .placeCategory(PlaceCategory.TRADITIONAL_MARKET)
                .title("지역 특산물 체험하기 1")
                .description("지역 특산물 첫 번째 체험 미션")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(0)
                .verificationType(VerificationType.PHOTO)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.SPECIALTY)
                .placeCategory(PlaceCategory.TRADITIONAL_MARKET)
                .title("지역 특산물 체험하기 2")
                .description("지역 특산물 두 번째 체험 미션")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(0)
                .verificationType(VerificationType.PHOTO)
                .build());
    }

    // 카테고리별 목록 조회 (없으면 자동 생성 후 반환)
    public List<UserMission> listByCategory(User user, MissionCategory category) {
        List<UserMission> missions = userMissionRepository.findMissionsByUserAndCategory(user, category);

        if (missions.isEmpty()) {
            initRegionMissions(user);
            missions = userMissionRepository.findMissionsByUserAndCategory(user, category);
        }

        return missions;
    }

    // 카테고리별 단건 조회
    public UserMission getOneByCategory(User user, Long id, MissionCategory category) {
        return userMissionRepository.findMissionByIdAndUserAndCategory(id, user, category)
                .orElseThrow(() -> new IllegalArgumentException("미션이 없거나 권한이 없습니다. id=" + id));
    }
}

