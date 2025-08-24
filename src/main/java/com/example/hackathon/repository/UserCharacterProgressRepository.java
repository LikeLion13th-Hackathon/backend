// repository/UserCharacterProgressRepository.java
package com.example.hackathon.repository;

import com.example.hackathon.entity.UserCharacterProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCharacterProgressRepository extends JpaRepository<UserCharacterProgress, Long> {
    Optional<UserCharacterProgress> findByUserIdAndSkinId(Integer userId, Long skinId);
    boolean existsByUserIdAndSkinId(Integer userId, Long skinId);
}
