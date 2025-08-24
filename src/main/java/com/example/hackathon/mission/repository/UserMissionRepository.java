package com.example.hackathon.mission.repository;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.MissionStatus;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

        // 기존
        List<UserMission> findByUserAndCategoryOrderByCreatedAtAsc(User user, MissionCategory category);

        boolean existsByUser(User user);

        Optional<UserMission> findByIdAndUser(Long id, User user);

        // 장소 분류별 목록 (createdAt 오름차순 정렬)
        List<UserMission> findByUserAndCategoryAndPlaceCategoryOrderByCreatedAtAsc(
                        User user, MissionCategory category, PlaceCategory placeCategory);

        // 단건 + 장소 분류 일치 확인용
        Optional<UserMission> findByIdAndUserAndPlaceCategory(Long id, User user, PlaceCategory placeCategory);

        // 단건 상세도 category로 검증
        Optional<UserMission> findByIdAndUserAndCategory(Long id, User user, MissionCategory category);

        // 카테고리만으로 전체 조회
        List<UserMission> findByCategory(MissionCategory category);

        // 여러 카테고리 조회
        List<UserMission> findByCategoryIn(List<MissionCategory> categories);

        // 목록
        @Query("SELECT um FROM UserMission um " +
                        "WHERE um.user = :user AND um.category = :category " +
                        "ORDER BY um.createdAt ASC")
        List<UserMission> findMissionsByUserAndCategory(@Param("user") User user,
                        @Param("category") MissionCategory category);

        // 단건
        @Query("SELECT um FROM UserMission um " +
                        "WHERE um.id = :id AND um.user = :user AND um.category = :category")
        Optional<UserMission> findMissionByIdAndUserAndCategory(@Param("id") Long id,
                        @Param("user") User user,
                        @Param("category") MissionCategory category);

        boolean existsByUserAndCategory(User user, MissionCategory category);

        // 유저별 완료된 미션 개수 카운트
        @Query("SELECT COUNT(um) FROM UserMission um " +
                        "WHERE um.user = :user AND um.status = com.example.hackathon.mission.entity.MissionStatus.COMPLETED")
        int countCompletedByUser(@Param("user") User user);

        List<UserMission> findByUserAndCategory(User user, MissionCategory category);

        void deleteByUserAndCategory(User user, MissionCategory category);

        // 특정 카테고리별 조회
        List<UserMission> findByUserAndCategoryAndPlaceCategory(User user, MissionCategory category, PlaceCategory placeCategory);
}
