// src/main/java/com/example/hackathon/security/JwtTokenProvider.java
package com.example.hackathon.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final javax.crypto.SecretKey key;
    private final long validitySeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:DEV_DEFAULT_SECRET_SHOULD_BE_32+_BYTES_1234567890}") String secret,
            @Value("${app.jwt.access-token-validity-seconds:7200}") long validitySeconds) {
        byte[] raw = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes. current=" + raw.length);
        }
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(raw);
        this.validitySeconds = validitySeconds;
    }

    public Claims parseClaims(String token) {
        try {
            // JJWT 0.12.x
            return Jwts.parser()
                    .verifyWith(key) // SecretKey key
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 payload는 꺼내 쓰게 허용
            return e.getClaims();
        }
    }

    public String generateToken(String email, Integer userId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(validitySeconds);

        return Jwts.builder()
                .subject(email)
                .claims(Map.of("uid", userId, "role", role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String getEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
