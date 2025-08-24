package com.example.hackathon.receipt.repository;

import com.example.hackathon.mission.entity.UserMission;
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

  // 월별 합계 (createdAt 기준, amount 합계 + 미션 개수 포함)
  @Query("""
          select function('date_format', r.createdAt, '%Y-%m') as month,
                 sum(r.amount),
                 count(distinct r.userMission.id)
          from Receipt r
          where r.user.id = :userId
            and r.ocrStatus = com.example.hackathon.receipt.OcrStatus.SUCCEEDED
            and r.amount is not null
          group by function('date_format', r.createdAt, '%Y-%m')
          order by month
      """)
  List<Object[]> sumAmountAndCountByMonth(@Param("userId") Long userId);

  // 월별 + 카테고리별 합계 (createdAt 기준)
  @Query("""
          select function('date_format', r.createdAt, '%Y-%m') as month,
                 r.detectedPlaceCategory,
                 sum(r.amount),
                 count(distinct r.userMission.id)
          from Receipt r
          where r.user.id = :userId
            and r.ocrStatus = com.example.hackathon.receipt.OcrStatus.SUCCEEDED
            and r.amount is not null
            and r.detectedPlaceCategory is not null
          group by function('date_format', r.createdAt, '%Y-%m'), r.detectedPlaceCategory
          order by month, r.detectedPlaceCategory
      """)
  List<Object[]> sumAmountByMonthAndCategory(@Param("userId") Long userId);

  // 평균 소비 금액
  @Query("""
          select avg(r.amount)
          from Receipt r
          where r.user.id = :userId
            and r.ocrStatus = com.example.hackathon.receipt.OcrStatus.SUCCEEDED
            and r.amount is not null
      """)
  Double averageAmount(@Param("userId") Long userId);

  void deleteAllByUserMission(UserMission mission);

}
