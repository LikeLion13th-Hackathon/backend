package com.example.hackathon.repository;

import com.example.hackathon.entity.CharacterSkin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CharacterSkinRepository extends JpaRepository<CharacterSkin, Long> {

    @Query("select s from CharacterSkin s where s.isActive = true")
    List<CharacterSkin> findAllActive();
}
