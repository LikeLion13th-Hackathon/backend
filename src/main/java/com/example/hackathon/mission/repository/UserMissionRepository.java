package com.example.hackathon.mission.repository;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.entity.VerificationType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

        // 기존
        List<UserMission> findByUserAndCategoryOrderByCreatedAtAsc(User user, MissionCategory category);

        boolean existsByUser(User user);

        Optional<UserMission> findByIdAndUser(Long id, User user);

        // 추가: 장소 분류별 목록 (createdAt 오름차순 정렬)
        List<UserMission> findByUserAndCategoryAndPlaceCategoryOrderByCreatedAtAsc(
                        User user, MissionCategory category, PlaceCategory placeCategory);

        // 추가: 단건 + 장소 분류 일치 확인용
        Optional<UserMission> findByIdAndUserAndPlaceCategory(Long id, User user, PlaceCategory placeCategory);

        // 추가: 단건 상세도 category로 검증
        Optional<UserMission> findByIdAndUserAndCategory(Long id, User user, MissionCategory category);

        // 최근 완료 N건의 placeCategory만 뽑기 위함
        long countByUserAndStatus(User user, MissionStatus status);

        List<UserMission> findTop20ByUserAndStatusOrderByCompletedAtDesc(
                        User user, MissionStatus status);

        boolean existsByUserAndCategoryAndPlaceCategoryAndVerificationType(
                        User user, MissionCategory category, PlaceCategory placeCategory, VerificationType type);
}
