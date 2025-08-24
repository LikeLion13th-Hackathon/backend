// src/main/java/com/example/hackathon/controller/RankingController.java
package com.example.hackathon.controller;

import com.example.hackathon.dto.LeaderboardResponse;
import com.example.hackathon.dto.MyRankResponse;
import com.example.hackathon.service.RankingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/top100")
    public LeaderboardResponse top100() {
        return rankingService.getTop100();
    }

    // 헤더/쿼리/시큐리티/세션 순으로 userId 탐색
    @GetMapping("/me")
    public MyRankResponse myRank(
            @RequestHeader(value = "X-User-Id", required = false) Integer headerUserId,
            @RequestParam(value = "userId", required = false) Integer queryUserId,
            HttpSession session
    ) {
        Integer uid = resolveUserId(headerUserId, queryUserId, session);
        if (uid == null) throw new IllegalStateException("userId 확인 불가(세션/헤더/토큰).");
        return rankingService.getMyRankWithNeighbors8(uid);
    }

    private Integer resolveUserId(Integer headerUserId, Integer queryUserId, HttpSession session) {
        // 1) Header 우선 (Postman용)
        if (headerUserId != null) return headerUserId;

        // 2) Query param (?userId=)
        if (queryUserId != null) return queryUserId;

        // 3) SecurityContext(JWT/커스텀 UserDetails)에서 꺼내보기
        Integer fromSec = tryFromSecurityContext();
        if (fromSec != null) return fromSec;

        // 4) Session
        Object v = session == null ? null : session.getAttribute("userId");
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return l.intValue();

        return null;
    }

    @SuppressWarnings("unchecked")
    private Integer tryFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal == null) return null;

        // (a) 커스텀 UserDetails에 getId()/getUserId()가 있는 경우
        try {
            for (String m : new String[]{"getId", "getUserId"}) {
                Method mm = principal.getClass().getMethod(m);
                Object val = mm.invoke(principal);
                if (val instanceof Integer i) return i;
                if (val instanceof Long l) return l.intValue();
                if (val instanceof String s && s.matches("\\d+")) return Integer.parseInt(s);
            }
        } catch (Exception ignore) { /* fallback 진행 */ }

        // (b) principal이 Map/JWT 클레임처럼 동작하는 경우
        try {
            if (principal instanceof Map<?, ?> map) {
                Object v = map.get("userId");
                if (v instanceof Integer i) return i;
                if (v instanceof Long l) return l.intValue();
                if (v instanceof String s && s.matches("\\d+")) return Integer.parseInt(s);
            }
        } catch (Exception ignore) { /* fallback 진행 */ }

        // (c) auth.getDetails()에 담긴 경우
        try {
            Object details = auth.getDetails();
            if (details instanceof Map<?, ?> map) {
                Object v = map.get("userId");
                if (v instanceof Integer i) return i;
                if (v instanceof Long l) return l.intValue();
                if (v instanceof String s && s.matches("\\d+")) return Integer.parseInt(s);
            }
        } catch (Exception ignore) { /* fallback 진행 */ }

        return null;
    }
}
