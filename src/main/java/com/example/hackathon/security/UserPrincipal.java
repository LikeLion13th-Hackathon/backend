// // src/main/java/com/example/hackathon/security/UserPrincipal.java
// package com.example.hackathon.security;

// import org.springframework.security.core.GrantedAuthority;

// import java.security.Principal;
// import java.util.Collection;

// /**
//  * JWT에서 꺼낸 사용자 정보(	id/email/권한	)를 담는 간단한 Principal.
//  * 컨트롤러에서 @AuthenticationPrincipal 로 바로 주입받아 사용한다.
//  */
// public record UserPrincipal(
//         Integer id,
//         String email,
//         Collection<? extends GrantedAuthority> authorities
// ) implements Principal {
//     @Override public String getName() { return email; }
// }