package com.example.hackathon.ai_custom.repository;

import com.example.hackathon.mission.entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiUserMissionRepository extends JpaRepository<UserMission, Long> {

    // 해당 유저의 완료된 미션 개수
    @Query("SELECT COUNT(m) FROM UserMission m " +
            "WHERE m.user.id = :userId " +
            "AND m.status = com.example.hackathon.mission.entity.MissionStatus.COMPLETED")
    int countByUserIdAndStatusCompleted(@Param("userId") Long userId);

    // 해당 유저의 완료된 미션 목록 (최근 완료 순)
    @Query("SELECT m FROM UserMission m " +
            "WHERE m.user.id = :userId " +
            "AND m.status = com.example.hackathon.mission.entity.MissionStatus.COMPLETED " +
            "ORDER BY m.completedAt DESC")
    List<UserMission> findCompletedMissions(@Param("userId") Long userId);

    // AI_CUSTOM 미션 목록 조회 (최신순)
    @Query("SELECT m FROM UserMission m " +
            "WHERE m.user.id = :userId " +
            "AND m.category = com.example.hackathon.mission.entity.MissionCategory.AI_CUSTOM " +
            "ORDER BY m.createdAt DESC")
    List<UserMission> findAiCustomMissions(@Param("userId") Long userId);

    // 특정 AI_CUSTOM 미션 단건 조회
    @Query("SELECT m FROM UserMission m " +
            "WHERE m.user.id = :userId " +
            "AND m.category = com.example.hackathon.mission.entity.MissionCategory.AI_CUSTOM " +
            "AND m.id = :missionId")
    Optional<UserMission> findAiCustomMissionById(@Param("userId") Long userId,
                                                  @Param("missionId") Long missionId);

    /*
     AI_CUSTOM 미션 INSERT (status=READY)
     start_date/end_date를 함께 저장 (기본 7일 유효기간)
     컬럼이 DATE 타입이면 CURDATE() 사용, DATETIME이면 NOW()로 교체
        */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO user_mission
              (user_id, category, place_category, title, description,
               verification_type, min_amount, reward_point, status,
               start_date, end_date, created_at)
            VALUES
              (:userId, 'AI_CUSTOM', :placeCategory, :title, :description,
               'PHOTO', 1, 50, 'READY',
               CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), NOW())
            """, nativeQuery = true)
    void insertAiCustomMission(@Param("userId") Long userId,
                               @Param("placeCategory") String placeCategory,  // 예: "CAFE"
                               @Param("title") String title,
                               @Param("description") String description);
}
