package com.example.hackathon.repository;

import com.example.hackathon.entity.Background;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BackgroundRepository extends JpaRepository<Background, Long> {
  @Query("select b from Background b where b.isActive = true")
  List<Background> findAllActive();
}
