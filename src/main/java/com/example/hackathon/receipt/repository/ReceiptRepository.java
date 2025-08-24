package com.example.hackathon.receipt.repository;

import com.example.hackathon.receipt.OcrStatus;
import com.example.hackathon.receipt.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    // userId를 Long으로 통일
    Optional<Receipt> findByIdAndUser_Id(Long id, Long userId);

    List<Receipt> findByUserMission_IdOrderByIdDesc(Long userMissionId);

    long countByUserMission_IdAndOcrStatusIn(Long userMissionId, List<OcrStatus> statuses);

    // ==================== 소비 패턴 집계 쿼리 ====================

    /** 카테고리별 카운트 (OCR 성공만) -> [0]=detectedPlaceCategory, [1]=count */
    @Query("""
           select r.detectedPlaceCategory, count(r)
           from Receipt r
           where r.user.id = :userId
             and r.ocrStatus = com.example.hackathon.receipt.OcrStatus.SUCCEEDED
             and r.detectedPlaceCategory is not null
           group by r.detectedPlaceCategory
           """)
    List<Object[]> countByCategory(@Param("userId") Long userId);

    /** 시간(0~23)별 카운트 (OCR 성공만) -> [0]=hour, [1]=count */
    @Query("""
           select function('hour', r.purchaseAt), count(r)
           from Receipt r
           where r.user.id = :userId
             and r.ocrStatus = com.example.hackathon.receipt.OcrStatus.SUCCEEDED
             and r.purchaseAt is not null
           group by function('hour', r.purchaseAt)
           order by function('hour', r.purchaseAt)
           """)
    List<Object[]> countByHour(@Param("userId") Long userId);
}
