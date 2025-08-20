package com.example.hackathon.service;

import com.example.hackathon.entity.Coin;
import com.example.hackathon.entity.User;
import com.example.hackathon.repository.CoinsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoinService {

    private final CoinsRepository coinsRepository;

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
}
