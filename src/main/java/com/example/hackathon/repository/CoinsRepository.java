package com.example.hackathon.repository;

import com.example.hackathon.entity.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoinsRepository extends JpaRepository<Coin, Integer> {

  // user_id 기준 잔액 차감. coins 테이블에 user_id가 있어야 합니다(UNIQUE 권장).
  @Modifying
  @Query(value = """
      UPDATE coins
         SET balance = balance - :cost
       WHERE user_id = :userId
         AND balance >= :cost
      """, nativeQuery = true)
  int tryDeduct(@Param("userId") Integer userId, @Param("cost") int cost);
}
