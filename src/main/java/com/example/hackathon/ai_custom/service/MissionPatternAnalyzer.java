package com.example.hackathon.ai_custom.service;

import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.receipt.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/*
 * 사용자 최근 소비 패턴을 간단 집계:
 *  - 카테고리 Top-N
 *  - 주요 시간대(오전/오후/저녁/야간)
 * ReceiptRepository의 집계 쿼리(countByCategory, countByHour)를 사용한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MissionPatternAnalyzer {

    private final ReceiptRepository receiptRepository;

    /*
     * @param userId 대상 사용자
     * @param topN   상위 카테고리 개수 (기본 3 추천)
     */
    public Result analyze(Long userId, int topN) {
        // 1) 카테고리별 카운트
        Map<PlaceCategory, Long> byCategory = receiptRepository.countByCategory(userId).stream()
                .filter(arr -> arr != null && arr.length >= 2)
                .collect(Collectors.toMap(
                        arr -> (PlaceCategory) arr[0],
                        arr -> (Long) arr[1]
                ));

        // 상위 N 카테고리
        List<PlaceCategory> topCategories = byCategory.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();

        // 2) 시간대별 카운트 (HOUR 기준)
        Map<Integer, Long> byHour = receiptRepository.countByHour(userId).stream()
                .filter(arr -> arr != null && arr.length >= 2)
                .collect(Collectors.toMap(
                        arr -> ((Number) arr[0]).intValue(),
                        arr -> (Long) arr[1]
                ));

        // 시간대를 밴드로 묶어서 가장 강한 밴드 찾기
        Map<HourBand, Long> bandCount = new EnumMap<>(HourBand.class);
        for (Map.Entry<Integer, Long> e : byHour.entrySet()) {
            HourBand band = HourBand.of(e.getKey());
            bandCount.merge(band, e.getValue(), Long::sum);
        }
        HourBand peakBand = bandCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(HourBand.AFTERNOON); // 기본값

        log.info("[AI] Pattern analyze userId={}, top={}, peakBand={}, rawCat={}, rawHour={}",
                userId, topCategories, peakBand, byCategory, byHour);

        return new Result(topCategories, peakBand, byCategory, byHour);
    }

    /* 시간대 밴드: 오전/오후/저녁/야간 */
    public enum HourBand {
        MORNING,     // 06-11
        AFTERNOON,   // 12-17
        EVENING,     // 18-22
        NIGHT;       // 23-05

        public static HourBand of(int hour) {
            if (hour >= 6 && hour <= 11) return MORNING;
            if (hour >= 12 && hour <= 17) return AFTERNOON;
            if (hour >= 18 && hour <= 22) return EVENING;
            // 23,0,1,2,3,4,5
            return NIGHT;
        }
    }

    @Value
    public static class Result {
        List<PlaceCategory> topCategories;       // 상위 N 카테고리 (기본 3)
        HourBand peakHourBand;                   // 주요 시간대 밴드
        Map<PlaceCategory, Long> rawCategory;    // 카테고리별 카운트
        Map<Integer, Long> rawHour;              // 시간(0-23)별 카운트
    }
}
