package com.example.hackathon.service;

import com.example.hackathon.dto.LeaderboardEntry;
import com.example.hackathon.dto.LeaderboardResponse;
import com.example.hackathon.dto.MyRankResponse;
import com.example.hackathon.repository.LeaderboardProjection;
import com.example.hackathon.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingRepository rankingRepository;

    /** TOP100 고정 (레포 쿼리: ORDER BY completed_count DESC, user_id ASC) */
    @Transactional(readOnly = true)
    public LeaderboardResponse getTop100() {
        var rows = rankingRepository.findTopN(100);
        var list = rows.stream().map(this::toEntry).toList();
        Integer total = rankingRepository.countParticipants();
        return LeaderboardResponse.builder()
                .totalParticipants(total == null ? 0 : total)
                .top(list)
                .build();
    }

    /** 자신 포함 8명: 위 3 + 나 + 아래 4 (가장자리 자동 보정) */
    @Transactional(readOnly = true)
    public MyRankResponse getMyRankWithNeighbors8(int userId) {
        Integer total = rankingRepository.countParticipants();
        if (total == null) total = 0;

        var myOpt = rankingRepository.findMyRankRow(userId); // RANK() OVER (ORDER BY cnt DESC, user_id ASC)

        // 완료가 없어서 랭킹이 없다면: TOP 8 반환, myRank=null
        if (myOpt.isEmpty()) {
            int to = Math.min(8, total);
            var top8 = (to > 0)
                    ? rankingRepository.findAround(1, to).stream().map(this::toEntry).toList()
                    : List.<LeaderboardEntry>of();

            return MyRankResponse.builder()
                    .myRank(null)
                    .myCompletedCount(0)
                    .totalParticipants(total)
                    .around(top8)
                    .build();
        }

        var my = myOpt.get();
        int myRank = my.getRnk();

        // 기본 범위: 위 3, 아래 4
        int from = Math.max(1, myRank - 3);
        int to   = Math.min(total, myRank + 4);

        // 총 8명으로 보정
        int length = to - from + 1;
        if (length < 8) {
            int need = 8 - length;

            // 아래쪽으로 먼저 늘려보고
            int canExtendDown = Math.max(0, total - to);
            int addDown = Math.min(need, canExtendDown);
            to += addDown;
            need -= addDown;

            // 남으면 위쪽으로 올림
            if (need > 0) {
                int canExtendUp = Math.max(0, from - 1);
                int addUp = Math.min(need, canExtendUp);
                from -= addUp;
            }
        }

        // 최종 조회
        var aroundRows = rankingRepository.findAround(from, to);
        var around = aroundRows.stream().map(this::toEntry).toList();

        return MyRankResponse.builder()
                .myRank(myRank)
                .myCompletedCount(my.getCompletedCount())
                .totalParticipants(total)
                .around(around)
                .build();
    }

    private LeaderboardEntry toEntry(LeaderboardProjection p) {
        return LeaderboardEntry.builder()
                .rank(p.getRnk())
                .userId(p.getUserId())
                .nickname(p.getNickname())
                .completedCount(p.getCompletedCount())
                .avatarUrl(null)
                .build();
    }
}
