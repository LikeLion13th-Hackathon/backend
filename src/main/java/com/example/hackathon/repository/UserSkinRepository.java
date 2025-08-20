package com.example.hackathon.repository;

import com.example.hackathon.entity.UserSkin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSkinRepository extends JpaRepository<UserSkin, Long> {

    List<UserSkin> findByUserId(Integer userId);

    boolean existsByUserIdAndSkinId(Integer userId, Long skinId);

    Optional<UserSkin> findByUserIdAndSkinId(Integer userId, Long skinId);
}
