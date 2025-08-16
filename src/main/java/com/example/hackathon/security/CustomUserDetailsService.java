package com.example.hackathon.security;

import com.example.hackathon.entity.User;
import com.example.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import org.springframework.security.core.GrantedAuthority;                
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user for email: " + email));

        // 간단히 role 문자열을 그대로 권한으로 사용
        List<GrantedAuthority> authorities =
                (u.getRole() != null && !u.getRole().isBlank())
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().toUpperCase()))
                        : List.of();

        return new org.springframework.security.core.userdetails.User(
                u.getEmail(),
                u.getPasswordHash(),
                authorities
        );
    }
}