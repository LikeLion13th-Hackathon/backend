package com.example.hackathon.ai_custom.controller;

import com.example.hackathon.ai_custom.dto.AiMissionResult;
import com.example.hackathon.ai_custom.service.AiMissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai-missions")
@RequiredArgsConstructor
public class AiMissionController {

    private final AiMissionService aiMissionService;

    /**
     * 사용자 맞춤 미션 3개 조회
     */
    @GetMapping("/{userId}")
    public List<AiMissionResult> getUserAiMissions(@PathVariable Long userId) {
        return aiMissionService.getUserAiMissions(userId);
    }

    /**
     * 맞춤 미션 단건 조회
     */
    @GetMapping("/{userId}/{missionId}")
    public AiMissionResult getUserAiMissionById(@PathVariable Long userId,
                                                @PathVariable Long missionId) {
        return aiMissionService.getUserAiMissionById(userId, missionId);
    }
}
