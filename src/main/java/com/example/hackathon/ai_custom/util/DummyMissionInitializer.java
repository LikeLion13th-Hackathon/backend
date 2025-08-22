// package com.example.hackathon.ai_custom.util;

// import jakarta.annotation.PostConstruct;
// import lombok.RequiredArgsConstructor;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.stereotype.Component;

// /**
//  * 더미 미션 데이터 초기화
//  */
// @Component
// @RequiredArgsConstructor
// public class DummyMissionInitializer {

//     private final JdbcTemplate jdbcTemplate;

//     @PostConstruct
//     public void init() {
//         String sql = "INSERT INTO user_mission (category, place_category, title, description, verification_type, min_amount, reward_point) VALUES (?, ?, ?, ?, ?, ?, ?)";

//         Object[][] dummyData = {
//                 {"CUSTOM", "CAFE", "카페에서 아메리카노 마시기", "근처 카페에서 아메리카노 주문 후 인증", "PHOTO", 1, 50},
//                 {"CUSTOM", "CAFE", "카페에서 독서하기", "카페에서 책을 읽는 사진 인증", "PHOTO", 1, 50},

//                 {"CUSTOM", "RESTAURANT", "식당에서 점심 식사", "식당에서 한 끼 인증", "PHOTO", 1, 70},
//                 {"CUSTOM", "RESTAURANT", "새로운 음식 도전", "평소 안 먹던 메뉴 먹기", "PHOTO", 1, 80},

//                 {"CUSTOM", "MUSEUM", "박물관 전시 관람", "전시 작품 앞에서 사진", "PHOTO", 1, 100},
//                 {"CUSTOM", "MUSEUM", "미술관 그림 감상", "마음에 드는 그림과 인증샷", "PHOTO", 1, 100},

//                 {"CUSTOM", "LIBRARY", "도서관 책 빌리기", "도서관에서 책 빌려오기", "PHOTO", 1, 30},
//                 {"CUSTOM", "LIBRARY", "조용히 공부하기", "도서관에서 공부하는 사진", "PHOTO", 1, 40},

//                 {"CUSTOM", "PARK", "공원 산책하기", "공원 걷는 사진 인증", "PHOTO", 1, 20},
//                 {"CUSTOM", "PARK", "운동하기", "공원에서 운동 인증샷", "PHOTO", 1, 30},

//                 {"CUSTOM", "SPORTS_FACILITY", "헬스장 가기", "운동 기구 사용하는 사진", "PHOTO", 1, 60},
//                 {"CUSTOM", "SPORTS_FACILITY", "새로운 운동 도전", "새로운 종목 도전 인증", "PHOTO", 1, 80},

//                 {"CUSTOM", "SHOPPING_MALL", "쇼핑하기", "쇼핑백 인증샷", "PHOTO", 1, 40},
//                 {"CUSTOM", "SHOPPING_MALL", "새 옷 입어보기", "구매한 옷 인증", "PHOTO", 1, 50},

//                 {"CUSTOM", "TRADITIONAL_MARKET", "시장 구경하기", "전통시장 풍경 인증", "PHOTO", 1, 20},
//                 {"CUSTOM", "TRADITIONAL_MARKET", "시장 음식 먹기", "시장 음식 인증샷", "PHOTO", 1, 30},

//                 {"CUSTOM", "OTHER", "기타 활동 참여", "특별한 장소에서 활동", "PHOTO", 1, 10},
//                 {"CUSTOM", "OTHER", "새로운 경험", "다양한 체험 인증", "PHOTO", 1, 20}
//         };

//         for (Object[] row : dummyData) {
//             jdbcTemplate.update(sql, row);
//         }
//     }
// }

package com.example.hackathon.ai_custom.util;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.entity.VerificationType;
import com.example.hackathon.mission.repository.UserMissionRepository;
import com.example.hackathon.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 더미 미션 데이터 초기화 (CUSTOM 카테고리)
 */
@Component
@RequiredArgsConstructor
public class DummyMissionInitializer {

    private final UserMissionRepository userMissionRepository;
    private final UserRepository userRepository;

    @PostConstruct
    @Transactional
    public void init() {
        // 🚨 테스트용 기본 유저 (없으면 생성)
        User user = userRepository.findByEmail("test@example.com")
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email("test@example.com")
                            // .password("1234")  // 실제 운영에서는 암호화 필수
                            .nickname("테스트유저")
                            .build();
                    return userRepository.save(newUser);
                });

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusWeeks(3);

        // ==== 더미 미션들 ====

        // ☕ CAFE
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.CAFE)
                .title("카페에서 아메리카노 마시기")
                .description("근처 카페에서 아메리카노 주문 후 인증")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(50)
                .startDate(start).endDate(end)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.CAFE)
                .title("카페에서 독서하기")
                .description("카페에서 책을 읽는 사진 인증")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(50)
                .startDate(start).endDate(end)
                .build());

        // 🍽 RESTAURANT
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.RESTAURANT)
                .title("식당에서 점심 식사")
                .description("식당에서 한 끼 인증")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(70)
                .startDate(start).endDate(end)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.RESTAURANT)
                .title("새로운 음식 도전")
                .description("평소 안 먹던 메뉴 먹기")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(80)
                .startDate(start).endDate(end)
                .build());

        // 🏛 MUSEUM
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.MUSEUM)
                .title("박물관 전시 관람")
                .description("전시 작품 앞에서 사진")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(100)
                .startDate(start).endDate(end)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.MUSEUM)
                .title("미술관 그림 감상")
                .description("마음에 드는 그림과 인증샷")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(100)
                .startDate(start).endDate(end)
                .build());

        // 📚 LIBRARY
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.LIBRARY)
                .title("도서관 책 빌리기")
                .description("도서관에서 책 빌려오기")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(30)
                .startDate(start).endDate(end)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.LIBRARY)
                .title("조용히 공부하기")
                .description("도서관에서 공부하는 사진")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(40)
                .startDate(start).endDate(end)
                .build());

        // 🌳 PARK
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.PARK)
                .title("공원 산책하기")
                .description("공원 걷는 사진 인증")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(20)
                .startDate(start).endDate(end)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.PARK)
                .title("운동하기")
                .description("공원에서 운동 인증샷")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(30)
                .startDate(start).endDate(end)
                .build());

        // 🏋 SPORTS_FACILITY
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.SPORTS_FACILITY)
                .title("헬스장 가기")
                .description("운동 기구 사용하는 사진")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(60)
                .startDate(start).endDate(end)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.SPORTS_FACILITY)
                .title("새로운 운동 도전")
                .description("새로운 종목 도전 인증")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(80)
                .startDate(start).endDate(end)
                .build());

        // 🛍 SHOPPING_MALL
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.SHOPPING_MALL)
                .title("쇼핑하기")
                .description("쇼핑백 인증샷")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(40)
                .startDate(start).endDate(end)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.SHOPPING_MALL)
                .title("새 옷 입어보기")
                .description("구매한 옷 인증")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(50)
                .startDate(start).endDate(end)
                .build());

        // 🛒 TRADITIONAL_MARKET
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.TRADITIONAL_MARKET)
                .title("시장 구경하기")
                .description("전통시장 풍경 인증")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(20)
                .startDate(start).endDate(end)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.TRADITIONAL_MARKET)
                .title("시장 음식 먹기")
                .description("시장 음식 인증샷")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(30)
                .startDate(start).endDate(end)
                .build());

        // 🎯 OTHER
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.OTHER)
                .title("기타 활동 참여")
                .description("특별한 장소에서 활동")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(10)
                .startDate(start).endDate(end)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.CUSTOM)
                .placeCategory(PlaceCategory.OTHER)
                .title("새로운 경험")
                .description("다양한 체험 인증")
                .status(MissionStatus.READY)
                .verificationType(VerificationType.PHOTO)
                .minAmount(1)
                .rewardPoint(20)
                .startDate(start).endDate(end)
                .build());
    }
}
