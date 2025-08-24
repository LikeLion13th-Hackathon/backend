// src/main/java/com/example/hackathon/dto/SkinActivateResult.java
package com.example.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스킨 활성화 결과 DTO
 * - 어떤 스킨이 활성화되었는지 (activeSkinId)
 * - 활성화된 스킨의 상세 정보 (SkinDTO)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SkinActivateResult {
    private Long activeSkinId;  // 현재 활성 스킨 ID
    private SkinDTO skin;       // 방금 활성화된 스킨 DTO
}
