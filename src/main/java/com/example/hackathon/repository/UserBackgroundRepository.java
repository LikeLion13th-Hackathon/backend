package com.example.hackathon.repository;

import com.example.hackathon.entity.UserBackground;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBackgroundRepository extends JpaRepository<UserBackground, Long> {
  boolean existsByUserIdAndBackgroundId(Integer userId, Long backgroundId);
  List<UserBackground> findByUserId(Integer userId);
  Optional<UserBackground> findByUserIdAndBackgroundId(Integer userId, Long backgroundId);
}
