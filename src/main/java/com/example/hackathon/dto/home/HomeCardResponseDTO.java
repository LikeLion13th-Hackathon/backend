package com.example.hackathon.dto.home;

import com.example.hackathon.mission.dto.MissionResponse; // ✅ 추가
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeCardResponseDTO {
    private HomeCardDTO homeCard;
    private List<MissionResponse> missions; // 공용 DTO
}
