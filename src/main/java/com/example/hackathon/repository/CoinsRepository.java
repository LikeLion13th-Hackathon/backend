package com.example.hackathon.repository;

import com.example.hackathon.entity.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CoinsRepository extends JpaRepository<Coin, Long> { 

  // 코인 잔액 조회 (User FK로 탐색)
  Optional<Coin> findByUser_Id(Integer userId); 

  // user_id 기준 잔액 차감
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    UPDATE coins
       SET balance = balance - :cost
     WHERE user_id = :userId
       AND balance >= :cost
    """, nativeQuery = true)
  int tryDeduct(@Param("userId") Integer userId, @Param("cost") int cost);

}
