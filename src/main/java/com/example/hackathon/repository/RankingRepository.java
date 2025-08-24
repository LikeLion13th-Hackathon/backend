package com.example.hackathon.repository;

import com.example.hackathon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RankingRepository extends JpaRepository<User, Integer> {

    // TOP N (동점자: first_completed ASC, 그다음 user_id ASC)
    @Query(value = """
        SELECT t.rnk        AS rnk,
               t.user_id    AS userId,
               t.nickname   AS nickname,
               t.completed_count AS completedCount
        FROM (
          SELECT ROW_NUMBER() OVER (ORDER BY x.cnt DESC, x.first_completed ASC, u.id ASC) AS rnk,
                 u.id   AS user_id,
                 u.nickname,
                 x.cnt  AS completed_count
          FROM (
            SELECT um.user_id AS uid,
                   COUNT(*)   AS cnt,
                   MIN(um.completed_at) AS first_completed
            FROM user_mission um
            WHERE um.status = 'COMPLETED'
            GROUP BY um.user_id
          ) x
          JOIN `user` u ON u.id = x.uid
        ) t
        WHERE t.rnk <= :limit
        """, nativeQuery = true)
    List<LeaderboardProjection> findTopN(@Param("limit") int limit);

    // 내 한 줄 (동일한 ORDER BY)
    @Query(value = """
        SELECT * FROM (
          SELECT ROW_NUMBER() OVER (ORDER BY x.cnt DESC, x.first_completed ASC, u.id ASC) AS rnk,
                 u.id   AS userId,
                 u.nickname,
                 x.cnt  AS completedCount
          FROM (
            SELECT um.user_id AS uid,
                   COUNT(*)   AS cnt,
                   MIN(um.completed_at) AS first_completed
            FROM user_mission um
            WHERE um.status = 'COMPLETED'
            GROUP BY um.user_id
          ) x
          JOIN `user` u ON u.id = x.uid
        ) t
        WHERE t.userId = :userId
        """, nativeQuery = true)
    Optional<LeaderboardProjection> findMyRankRow(@Param("userId") int userId);

    // 내 주변 구간 (fromRank ~ toRank)
    @Query(value = """
        WITH ranked AS (
          SELECT ROW_NUMBER() OVER (ORDER BY x.cnt DESC, x.first_completed ASC, u.id ASC) AS rnk,
                 u.id   AS userId,
                 u.nickname,
                 x.cnt  AS completedCount
          FROM (
            SELECT um.user_id AS uid,
                   COUNT(*)   AS cnt,
                   MIN(um.completed_at) AS first_completed
            FROM user_mission um
            WHERE um.status = 'COMPLETED'
            GROUP BY um.user_id
          ) x
          JOIN `user` u ON u.id = x.uid
        )
        SELECT rnk AS rnk, userId AS userId, nickname AS nickname, completedCount AS completedCount
        FROM ranked
        WHERE rnk BETWEEN :fromRank AND :toRank
        """, nativeQuery = true)
    List<LeaderboardProjection> findAround(@Param("fromRank") int fromRank,
                                           @Param("toRank") int toRank);

    // 참여자 수 (완료 경험 있는 사용자 수)
    @Query(value = "SELECT COUNT(DISTINCT um.user_id) FROM user_mission um WHERE um.status = 'COMPLETED'", nativeQuery = true)
    Integer countParticipants();
}
