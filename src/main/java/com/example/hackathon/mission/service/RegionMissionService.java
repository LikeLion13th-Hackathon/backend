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

        // 지역 맛집 (RESTAURANT)
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.RESTAURANT)
                .placeCategory(PlaceCategory.RESTAURANT)
                .title("연안부두 전설의 회")
                .description("바다의 힘을 흡수해 공격력을 상승시키세요! 횟집에서 회를 먹은 후 영수증으로 인증해주세요!")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(5000)
                .verificationType(VerificationType.RECEIPT_OCR)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.RESTAURANT)
                .placeCategory(PlaceCategory.CAFE)
                .title("카페거리 감성 버프")
                .description("송도의 감성이 캐릭터의 매력치를 채워준답니다~ 송도 카페에서 시그니처 음료를 구매한 후 영수증으로 인증해주세요!")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(5000)
                .verificationType(VerificationType.RECEIPT_OCR)
                .build());

        // 지역 명소 (LANDMARK)
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.LANDMARK)
                .placeCategory(PlaceCategory.CAFE)
                .title("센트럴파크 버프 아이템 구매")
                .description("센트럴파크 인근 카페에서 음료와 샌드위치 세트를 구매 후 영수증으로 인증해주세요!")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(0)
                .verificationType(VerificationType.RECEIPT_OCR)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.LANDMARK)
                .placeCategory(PlaceCategory.TRADITIONAL_MARKET)
                .title("트리플 스트리트 상인과의 거래")
                .description("상인과의 거래에 성공하면 희귀 코인이 떨어진답니다~ 로컬 브랜드 매장에서 1만원 이상 소비 후 영수증으로 인증해주세요!")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(0)
                .verificationType(VerificationType.RECEIPT_OCR)
                .build());

        // 특산물 (SPECIALTY)
        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.SPECIALTY)
                .placeCategory(PlaceCategory.TRADITIONAL_MARKET)
                .title("젓갈 보관함 개방")
                .description("짭짤한 아이템을 획득하여 코인을 획득해보세요~ 전통시장에서 젓갈류를 구매 후 영수증으로 인증해주세요!")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(0)
                .verificationType(VerificationType.RECEIPT_OCR)
                .build());

        userMissionRepository.save(UserMission.builder()
                .user(user)
                .category(MissionCategory.SPECIALTY)
                .placeCategory(PlaceCategory.TRADITIONAL_MARKET)
                .title("강화 순무 파워 스톤")
                .description("강화도의 땅 기운이 깃든 스톤, 방어력을 올려줍니다^^ 전통 시장에서 강화 순무 김치를 구매 후 영수증으로 인증해주세요!")
                .status(MissionStatus.READY)
                .startDate(start)
                .endDate(end)
                .rewardPoint(200)
                .minAmount(0)
                .verificationType(VerificationType.RECEIPT_OCR)
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

