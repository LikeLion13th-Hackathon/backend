package com.example.hackathon.mission.service;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.entity.VerificationType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RegionMissionService {

    // ID 시퀀스 (메모리 기반)
    private static final AtomicLong SEQ = new AtomicLong(100);

    // 카테고리별 더미 데이터 생성
    private List<UserMission> buildDummyMissions(User user, MissionCategory category) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusWeeks(3);

        return switch (category) {
            case RESTAURANT -> List.of(
                    UserMission.builder()
                            .id(SEQ.incrementAndGet())
                            .user(user)
                            .category(MissionCategory.RESTAURANT)
                            .placeCategory(PlaceCategory.RESTAURANT)
                            .title("서대문구 북가좌동 맛집 결제 인증 (A)")
                            .description("현지 맛집 방문 후 영수증 업로드 시 자동 인증됩니다.")
                            .verificationType(VerificationType.RECEIPT_OCR)
                            .minAmount(5000)
                            .rewardPoint(200)
                            .status(MissionStatus.READY)
                            .startDate(start)
                            .endDate(end)
                            .createdAt(LocalDateTime.now())
                            .build(),
                    UserMission.builder()
                            .id(SEQ.incrementAndGet())
                            .user(user)
                            .category(MissionCategory.RESTAURANT)
                            .placeCategory(PlaceCategory.RESTAURANT)
                            .title("서대문구 북가좌동 맛집 사진 인증 (B)")
                            .description("메뉴판/가게 전경 사진 인증 시 즉시 완료됩니다.")
                            .verificationType(VerificationType.PHOTO)
                            .rewardPoint(120)
                            .status(MissionStatus.READY)
                            .startDate(start)
                            .endDate(end)
                            .createdAt(LocalDateTime.now())
                            .build()
            );
            case LANDMARK -> List.of(
                    UserMission.builder()
                            .id(SEQ.incrementAndGet())
                            .user(user)
                            .category(MissionCategory.LANDMARK)
                            .placeCategory(PlaceCategory.PARK)
                            .title("서대문구 북가좌동 명소 사진 인증 (A)")
                            .description("북가좌동 공원/산책로 명소 사진 인증")
                            .verificationType(VerificationType.PHOTO)
                            .rewardPoint(100)
                            .status(MissionStatus.READY)
                            .startDate(start)
                            .endDate(end)
                            .createdAt(LocalDateTime.now())
                            .build(),
                    UserMission.builder()
                            .id(SEQ.incrementAndGet())
                            .user(user)
                            .category(MissionCategory.LANDMARK)
                            .placeCategory(PlaceCategory.PARK)
                            .title("서대문구 북가좌동 명소 영수증 인증 (B)")
                            .description("명소 인근 매점/부대시설 영수증 업로드 시 자동 인증")
                            .verificationType(VerificationType.RECEIPT_OCR)
                            .minAmount(2000)
                            .rewardPoint(150)
                            .status(MissionStatus.READY)
                            .startDate(start)
                            .endDate(end)
                            .createdAt(LocalDateTime.now())
                            .build()
            );
            case SPECIALTY -> List.of(
                    UserMission.builder()
                            .id(SEQ.incrementAndGet())
                            .user(user)
                            .category(MissionCategory.SPECIALTY)
                            .placeCategory(PlaceCategory.TRADITIONAL_MARKET)
                            .title("서대문구 전통시장 특산물 영수증 인증 (A)")
                            .description("특산물 구입 후 영수증 업로드 시 자동 인증됩니다.")
                            .verificationType(VerificationType.RECEIPT_OCR)
                            .minAmount(3000)
                            .rewardPoint(180)
                            .status(MissionStatus.READY)
                            .startDate(start)
                            .endDate(end)
                            .createdAt(LocalDateTime.now())
                            .build(),
                    UserMission.builder()
                            .id(SEQ.incrementAndGet())
                            .user(user)
                            .category(MissionCategory.SPECIALTY)
                            .placeCategory(PlaceCategory.TRADITIONAL_MARKET)
                            .title("서대문구 전통시장 특산물 사진 인증 (B)")
                            .description("시장 특산물 사진 인증 시 즉시 완료됩니다.")
                            .verificationType(VerificationType.PHOTO)
                            .rewardPoint(120)
                            .status(MissionStatus.READY)
                            .startDate(start)
                            .endDate(end)
                            .createdAt(LocalDateTime.now())
                            .build()
            );
            default -> List.of();
        };
    }

    // 목록 조회
    public List<UserMission> listByCategory(User user, MissionCategory category) {
        return buildDummyMissions(user, category);
    }

    // 단건 조회
    public UserMission getOneByCategory(User user, Long id, MissionCategory category) {
        return buildDummyMissions(user, category).stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("미션이 없거나 권한이 없습니다. id=" + id));
    }
}
