package com.example.hackathon.mypage.controller;

import com.example.hackathon.dto.mypage.MyPageResponseDTO;
import com.example.hackathon.dto.mypage.MyPageUpdateDTO;
import com.example.hackathon.mypage.service.MyPageService;
import com.example.hackathon.repository.UserRepository;
import com.example.hackathon.entity.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Validated
@Slf4j
public class MyPageController {

    private final MyPageService myPageService;
    private final UserRepository userRepository; // ✅ email→id 조회용

    /** 마이페이지 상단 - 내 정보 조회 */
    @GetMapping
    public MyPageResponseDTO getMyInfo(@AuthenticationPrincipal Object principal, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        return myPageService.getMyInfo(userId);
    }

    /** 마이페이지 상단 - 내 정보 수정 */
    @PatchMapping
    public MyPageResponseDTO updateMyInfo(@AuthenticationPrincipal Object principal,
                                          HttpSession session,
                                          @RequestBody @Valid MyPageUpdateDTO request) {
        Long userId = resolveUserId(principal, session);
        return myPageService.updateMyInfo(userId, request);
    }

    /** 진행 중 미션 */
    @GetMapping("/missions/in-progress")
    public List<String> inProgress(@AuthenticationPrincipal Object principal, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        return myPageService.getInProgressMissions(userId);
    }

    /** 완료 미션 */
    @GetMapping("/missions/completed")
    public List<String> completed(@AuthenticationPrincipal Object principal, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        return myPageService.getCompletedMissions(userId);
    }

    /** ✅ userId 해석 로직(보강) */
    private Long resolveUserId(Object principal, HttpSession session) {
        // 1) Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                && !"anonymousUser".equals(auth.getPrincipal())) {

            Object p = auth.getPrincipal();

            // (a) 커스텀 Principal: getId(), getUserId() 지원
            try {
                // getId()
                var m = p.getClass().getMethod("getId");
                m.setAccessible(true);
                Object idObj = m.invoke(p);
                if (idObj instanceof Integer i) return i.longValue();
                if (idObj instanceof Long l)    return l;
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {}

            try {
                // getUserId()
                var m = p.getClass().getMethod("getUserId");
                m.setAccessible(true);
                Object idObj = m.invoke(p);
                if (idObj instanceof Integer i) return i.longValue();
                if (idObj instanceof Long l)    return l;
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {}

            // (b) UserDetails: username = email 가정
            if (p instanceof UserDetails ud && ud.getUsername() != null) {
                return userRepository.findByEmail(ud.getUsername())
                        .map(u -> u.getId().longValue())
                        .orElse(null);
            }

            // (c) principal이 이메일 문자열인 경우
            if (p instanceof String s && s.contains("@")) {
                return userRepository.findByEmail(s)
                        .map(u -> u.getId().longValue())
                        .orElse(null);
            }
        }

        // 2) 세션에서 여러 키로 시도
        for (String key : new String[]{"userId", "id", "uid"}) {
            Object v = session.getAttribute(key);
            if (v instanceof Integer i) return i.longValue();
            if (v instanceof Long l)    return l;
            if (v instanceof String s) {
                try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
            }
        }

        // 3) 세션에 email만 있는 경우
        for (String key : new String[]{"loginEmail", "email", "userEmail"}) {
            Object ev = session.getAttribute(key);
            if (ev instanceof String es && es.contains("@")) {
                return userRepository.findByEmail(es)
                        .map(u -> u.getId().longValue())
                        .orElse(null);
            }
        }

        throw new IllegalStateException("인증 정보에서 userId를 확인할 수 없습니다.");
    }

}
