// src/main/java/com/example/hackathon/auth/CurrentUserResolver.java
package com.example.hackathon.auth;

import com.example.hackathon.common.ForbiddenException;
import com.example.hackathon.common.NotFoundException;
import com.example.hackathon.entity.User;
import com.example.hackathon.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public Integer resolveUserId(HttpServletRequest req) {
        // 1) Bearer JWT 우선
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

                // jjwt 0.12.x 스타일 파싱
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // uid(정수) 우선
                Object uidObj = claims.get("uid");
                if (uidObj != null) {
                    if (uidObj instanceof Number num) {
                        return num.intValue();
                    }
                    return Integer.valueOf(uidObj.toString());
                }

                // sub(email) fallback
                String email = claims.getSubject();
                if (email != null && !email.isBlank()) {
                    return userRepository.findByEmail(email)
                            .map(User::getId)
                            .orElseThrow(() -> new NotFoundException("user"));
                }
            } catch (JwtException e) {
                // 토큰 문제는 무시하고 다음 규칙으로 진행
            }
        }

        // 2) X-USER-EMAIL 헤더
        String email = req.getHeader("X-USER-EMAIL");
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElseThrow(() -> new NotFoundException("user"));
        }

        // 3) X-USER-ID 헤더 (임시/테스트용)
        String idHeader = req.getHeader("X-USER-ID");
        if (idHeader != null && !idHeader.isBlank()) {
            try {
                return Integer.parseInt(idHeader);
            } catch (NumberFormatException ignore) { /* fallthrough */ }
        }

        throw new ForbiddenException("unauthenticated");
    }
}
