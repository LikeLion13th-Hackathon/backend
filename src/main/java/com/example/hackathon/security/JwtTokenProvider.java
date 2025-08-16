// src/main/java/com/example/hackathon/security/JwtTokenProvider.java
package com.example.hackathon.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;                   
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final SecretKey key;              
    private final long validitySeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity-seconds:7200}") long validitySeconds
    ) {
        // HS256용 시크릿은 최소 32바이트 이상 권장
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = validitySeconds;
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
