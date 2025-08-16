package com.example.hackathon.repository;

import com.example.hackathon.entity.CharacterLevelRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LevelReqRepository extends JpaRepository<CharacterLevelRequirement, Integer> {

    // level 값으로 feeds_required 조회
    Optional<CharacterLevelRequirement> findByLevel(Integer level);
}
