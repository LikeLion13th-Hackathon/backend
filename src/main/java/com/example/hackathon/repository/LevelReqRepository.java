package com.example.hackathon.repository;

import com.example.hackathon.entity.CharacterLevelRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelReqRepository extends JpaRepository<CharacterLevelRequirement, Integer> { }
