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

    private final javax.crypto.SecretKey key;
    private final long validitySeconds;

    public JwtTokenProvider(
        @Value("${app.jwt.secret:DEV_DEFAULT_SECRET_SHOULD_BE_32+_BYTES_1234567890}") String secret,
        @Value("${app.jwt.access-token-validity-seconds:7200}") long validitySeconds
    ) {
        byte[] raw = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes. current=" + raw.length);
        }
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(raw);
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

// // src/main/java/com/example/hackathon/security/JwtTokenProvider.java
// package com.example.hackathon.security;

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.security.Keys;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;

// import javax.crypto.SecretKey;
// import java.nio.charset.StandardCharsets;
// import java.time.Instant;
// import java.util.Date;
// import java.util.Map;

// /**
//  * JWT 생성/검증/파싱 전담.
//  * - subject = email
//  * - claims: uid(Integer), role(String)
//  */
// @Component
// public class JwtTokenProvider {

//     private final SecretKey key;              // HS256 비밀키 (32바이트 이상)
//     private final long validitySeconds;       // 액세스 토큰 만료(초)

//     public JwtTokenProvider(
//             @Value("${app.jwt.secret:DEV_DEFAULT_SECRET_SHOULD_BE_32+_BYTES_1234567890}") String secret,
//             @Value("${app.jwt.access-token-validity-seconds:7200}") long validitySeconds
//     ) {
//         byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
//         if (raw.length < 32) {
//             throw new IllegalStateException(
//                 "app.jwt.secret must be at least 32 bytes for HS256. current=" + raw.length
//             );
//         }
//         this.key = Keys.hmacShaKeyFor(raw);
//         this.validitySeconds = validitySeconds;
//     }

//     /** 액세스 토큰 생성 */
//     public String generateToken(String email, Integer userId, String role) {
//         Instant now = Instant.now();
//         Instant exp = now.plusSeconds(validitySeconds);

//         return Jwts.builder()
//                 .subject(email)
//                 .claims(Map.of(
//                         "uid", userId,
//                         "role", role
//                 ))
//                 .issuedAt(Date.from(now))
//                 .expiration(Date.from(exp))
//                 .signWith(key)
//                 .compact();
//     }

//     /** 토큰 서명/만료 검증 */
//     public boolean validate(String token) {
//         try {
//             Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
//             return true;
//         } catch (Exception e) {
//             return false;
//         }
//     }

//     /** 클레임 전체 파싱 (subject/uid/role 등) */
//     public Claims parseClaims(String token) {
//         return Jwts.parser().verifyWith(key).build()
//                 .parseSignedClaims(token)
//                 .getPayload();
//     }
// }
