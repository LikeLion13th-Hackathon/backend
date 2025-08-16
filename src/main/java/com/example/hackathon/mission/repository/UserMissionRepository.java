package com.example.hackathon.mission.repository;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {
    List<UserMission> findByUserAndCategoryOrderByCreatedAtAsc(User user, MissionCategory category);
    boolean existsByUser(User user);
    Optional<UserMission> findByIdAndUser(Long id, User user);
}
