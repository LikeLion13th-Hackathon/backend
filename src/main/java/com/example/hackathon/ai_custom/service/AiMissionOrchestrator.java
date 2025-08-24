package com.example.hackathon.ai_custom.service;

/**
 * OCR 미션이 3개 단위로 완료될 때 호출되는 오케스트레이터.
 * 내부에서 소비 패턴 분석 → 프롬프트 생성 → LLM 호출 → 미션 4개(분석 3 + 신규 1) 생성까지 담당.
 */
public interface AiMissionOrchestrator {
    void recommendNextSet(Long userId);
}
