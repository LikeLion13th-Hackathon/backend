package com.example.hackathon.service;

import com.example.hackathon.entity.Coin;
import com.example.hackathon.entity.User;
import com.example.hackathon.repository.CoinsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoinService {

    private final CoinsRepository coinsRepository;

    // ===== 기존 메서드 (User 기반) =====
    @Transactional
    public void addCoins(User user, int amount) {
        Coin coin = coinsRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    Coin c = new Coin();
                    c.setUser(user);
                    c.setBalance(0);
                    return c;
                });
        coin.setBalance(coin.getBalance() + amount);
        coinsRepository.save(coin);
    }

    @Transactional(readOnly = true)
    public int getBalance(User user) {
        return coinsRepository.findByUser_Id(user.getId())
                .map(Coin::getBalance)
                .orElse(0);
    }

    // ===== 추가 메서드 (userId 기반) =====
    @Transactional
    public void addCoinsByUserId(Integer userId, int amount) {
        Coin coin = coinsRepository.findByUser_Id(userId)
                .orElseThrow(() -> new EntityNotFoundException("User coin wallet not found: " + userId));
        coin.setBalance(coin.getBalance() + amount);
        coinsRepository.save(coin);
    }

    @Transactional(readOnly = true)
    public int getBalanceByUserId(Integer userId) {
        return coinsRepository.findByUser_Id(userId)
                .map(Coin::getBalance)
                .orElse(0);
    }
}
