package com.example.hackathon.repository;

import com.example.hackathon.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {
  Optional<CharacterEntity> findByUserId(Integer userId);
}