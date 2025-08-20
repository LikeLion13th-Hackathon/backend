package com.example.hackathon.mission.service;

import com.example.hackathon.entity.User;
import com.example.hackathon.mission.entity.MissionCategory;
import com.example.hackathon.mission.entity.PlaceCategory;
import com.example.hackathon.mission.entity.UserMission;
import com.example.hackathon.mission.repository.UserMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionMissionService {

    private final UserMissionRepository userMissionRepository;

    // 목록: 카테고리 기준
    public List<UserMission> listByCategory(User user, MissionCategory category) {
        return userMissionRepository.findByUserAndCategoryOrderByCreatedAtAsc(user, category);
    }

    // 단건: 카테고리 기준
    public UserMission getOneByCategory(User user, Long id, MissionCategory category) {
        return userMissionRepository.findByIdAndUserAndCategory(id, user, category)
                .orElseThrow(() -> new IllegalArgumentException("미션이 없거나 권한이 없습니다. id=" + id));
    }
}
