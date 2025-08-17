package com.example.hackathon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService uds;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
        throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = null;

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        if (token != null && tokenProvider.validate(token)) {
            String email = tokenProvider.getEmail(token);
            UserDetails userDetails = uds.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}

// // src/main/java/com/example/hackathon/security/JwtAuthenticationFilter.java
// package com.example.hackathon.security;

// import io.jsonwebtoken.Claims;
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.*;
// import lombok.RequiredArgsConstructor;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.stereotype.Component;
// import org.springframework.web.filter.OncePerRequestFilter;

// import java.io.IOException;
// import java.util.List;

// /**
//  * 매 요청마다 Authorization: Bearer <JWT> 를 읽어
//  * - 유효하면 JWT 클레임(subject/uid/role) → UserPrincipal 로 변환
//  * - SecurityContext 에 Authentication 을 세팅
//  *   (DB 조회 없이 동작: 빠르고 단순)
//  */
// @Component
// @RequiredArgsConstructor
// public class JwtAuthenticationFilter extends OncePerRequestFilter {

//     private final JwtTokenProvider tokenProvider;

//     @Override
//     protected void doFilterInternal(HttpServletRequest request,
//                                     HttpServletResponse response,
//                                     FilterChain chain)
//         throws ServletException, IOException {

//         String header = request.getHeader("Authorization");
//         String token = (header != null && header.startsWith("Bearer "))
//                 ? header.substring(7) : null;

//         if (token != null && tokenProvider.validate(token)) {
//             Claims claims = tokenProvider.parseClaims(token);

//             String email = claims.getSubject();                   // subject = email
//             Number uidNum = claims.get("uid", Number.class);      // uid는 Number 로 받아 안전 변환
//             Integer uid = (uidNum != null) ? uidNum.intValue() : null;

//             String role = claims.get("role", String.class);
//             List<GrantedAuthority> authorities =
//                     (role == null || role.isBlank())
//                             ? List.of()
//                             : List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

//             // 🔑 DB 조회 없이 토큰에서 바로 Principal 구성
//             UserPrincipal principal = new UserPrincipal(uid, email, authorities);

//             UsernamePasswordAuthenticationToken auth =
//                     new UsernamePasswordAuthenticationToken(principal, null, authorities);

//             SecurityContextHolder.getContext().setAuthentication(auth);
//         }

//         chain.doFilter(request, response);
//     }
// }
