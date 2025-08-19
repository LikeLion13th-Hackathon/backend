// src/main/java/com/example/hackathon/receipt/repository/ReceiptRepository.java
package com.example.hackathon.receipt.repository;

import com.example.hackathon.receipt.entity.Receipt;
import com.example.hackathon.receipt.OcrStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByIdAndUser_Id(Long id, Integer userId);

    List<Receipt> findByUserMission_IdOrderByIdDesc(Long userMissionId);

    long countByUserMission_IdAndOcrStatusIn(Long userMissionId, List<OcrStatus> statuses);
}
