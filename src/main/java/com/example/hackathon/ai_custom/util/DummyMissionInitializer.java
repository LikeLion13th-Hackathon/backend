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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DummyMissionInitializer {

    private final UserMissionRepository userMissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // ✅ 주입

    @PostConstruct
    @Transactional
    public void init() {
        // ✅ 테스트 유저 생성 (없으면)
        User user = userRepository.findByEmail("test@example.com")
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email("test@example.com")
                            .nickname("테스트유저")
                            // ✅ 반드시 해시 채우기
                            .passwordHash(passwordEncoder.encode("changeme123!"))
                            // 아래는 DB 스키마에 맞게 필요 시 채우세요 (NOT NULL 컬럼들)
                            // .role(Role.USER)
                            // .privacyAgreed(true)
                            // .serviceAgreed(true)
                            // .pref1(PlaceCategory.OTHER)
                            // .pref2(PlaceCategory.OTHER)
                            // .pref3(PlaceCategory.OTHER)
                            .build();
                    return userRepository.save(newUser);
                });

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusWeeks(3);

        // 🚫 중복 방지: 이미 하나라도 있으면 스킵(선택)
        if (userMissionRepository.count() > 0) return;

        // ==== 이하 미션 생성(기존 코드 그대로) ====
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
