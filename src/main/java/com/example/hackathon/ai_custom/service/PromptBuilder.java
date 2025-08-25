package com.example.hackathon.ai_custom.service;

import com.example.hackathon.mission.entity.PlaceCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PromptBuilder {

    // 카테고리별 "대체/변형" 아이디어 풀
    private static final Map<String, List<String>> IDEA_POOL = new LinkedHashMap<>();

    static {
        IDEA_POOL.put("카페", Arrays.asList(
                "커피 대신 허브차/꿀차/레몬차 마시기",
                "평소 아아 대신 디카페인/콜드브루 선택",
                "테이크아웃 컵 대신 머그컵 사용",
                "텀블러 가져가서 할인받기",
                "음료 대신 디저트만 주문해서 소확행",
                "바닐라/헤이즐넛 등 평소 안 먹는 시럽 도전",
                "로컬 브랜드 카페 이용",
                "시그니처 메뉴 1회 도전"
        ));

        IDEA_POOL.put("식당", Arrays.asList(
                "고기 대신 채식/샐러드 메뉴 시도",
                "1만원 이하 메뉴 조합 만들기",
                "평소 한식 대신 다른 나라 음식 시도",
                "신메뉴 1개 도전하기",
                "1인분만 주문해서 남김 없이 먹기",
                "지역 맛집 신규 방문",
                "반찬 리필 없이 먹기",
                "저염/저칼로리 옵션 선택"
        ));

        IDEA_POOL.put("편의점", Arrays.asList(
                "과자 대신 견과류/프로틴바 선택",
                "제로/무가당 음료 골라보기",
                "신제품·한정판 음료 1개 찾기",
                "3000원 이하 건강 간식 세트 구성",
                "도시락 대신 샐러드 선택",
                "같은 음료 2개 대신 다른 음료 2개",
                "야식 대신 아침용 간식 준비",
                "뜨끈한 국물류로 간단히"
        ));

        IDEA_POOL.put("서점/문구점", Arrays.asList(
                "새 책 대신 중고책 구매",
                "평소 장르 말고 다른 장르 1권",
                "노트/펜 등 소액 문구류 구매",
                "30분 독서 후 인증",
                "선물용 문구 1개 고르기",
                "지출 기록용 노트 시작",
                "에세이/과학/경제 등 미경험 분야 도전",
                "북마크/스티커로 독서 동기부여"
        ));

        IDEA_POOL.put("박물관/미술관", Arrays.asList(
                "관람 후 3줄 감상평 남기기",
                "작품 스케치 1컷 인증",
                "입장권 대신 굿즈 소액 구매",
                "30분 이상 머물며 사진 인증",
                "혼자 관람 대신 동행과 함께",
                "작품 포즈 따라하기 사진",
                "도슨트 메모 적기",
                "근처 카페와 연계 관람 루틴"
        ));

        IDEA_POOL.put("공원", Arrays.asList(
                "산책 20분 → 30분으로 늘리기",
                "줍깅(쓰레기 줍기+조깅) 도전",
                "계단 오르기 챌린지",
                "특정 나무/조형물 찾기",
                "아침 산책 vs 야간 산책",
                "벤치에서 스트레칭 5분",
                "근처 카페와 세트 인증샷",
                "반려동물 산책 동행 요청"
        ));

        IDEA_POOL.put("기타", Arrays.asList(
                "동네 로컬 가게 랜덤 방문",
                "시장 골목 탐방",
                "포장 대신 매장에서 먹기",
                "영화관 대신 독립영화관",
                "동네 사진 3장 찍기",
                "음악 듣고 플레이리스트 인증",
                "오늘 소비를 메모/SNS로 정리",
                "현금 대신 지역화폐 써보기"
        ));
    }

    public String buildPrompt(Long userId, List<PlaceCategory> topCategories, MissionPatternAnalyzer.HourBand peakBand) {
        // 난수 시드 → 결과 다양성 힌트
        int seed = ThreadLocalRandom.current().nextInt(10_000, 99_999);

        // 표시용 카테고리 문자열
        List<String> topCatsStr = (topCategories == null || topCategories.isEmpty())
                ? Collections.emptyList()
                : topCategories.stream().map(Object::toString).collect(Collectors.toList());

        String allowList = topCatsStr.isEmpty()
                ? "카페, 식당, 편의점, 서점/문구점, 공원, 박물관/미술관, 기타"
                : String.join(", ", topCatsStr);

        StringBuilder sb = new StringBuilder(4000);

        // ===== 시스템 & 맥락 =====
        sb.append("당신은 지역 소비를 촉진하는 ‘미션 메이커 AI’입니다.\n")
                .append("사용자 ID: ").append(userId).append("\n")
                .append("최근 소비 상위 카테고리: ").append(topCatsStr).append("\n")
                .append("주요 소비 시간대(피크): ").append(peakBand).append("\n")
                .append("seed: ").append(seed).append("\n\n");

        // ===== 출력 스펙 =====
        sb.append("출력은 반드시 JSON 배열 **한 개**로만 반환하세요. 마크다운/설명/주석 금지.\n")
                .append("각 항목 스키마:\n")
                .append("[\n")
                .append("  {\n")
                .append("    \"title\": string(최대 30자, 이모지 0~2개 허용, 숫자 나열/AI 미션 N 금지),\n")
                .append("    \"description\": string(최대 120자, 구체적 행동/조건/팁 포함),\n")
                .append("    \"category\": string(아래 허용 목록 중 정확히 하나),\n")
                .append("    \"minAmount\": integer(0 또는 1000~20000에서 상황 맞춤),\n")
                .append("    \"rewardPoint\": integer(100~200 가변),\n")
                .append("    \"verificationType\": \"RECEIPT_OCR\"\n")
                .append("  }, ... 총 4개]\n\n");

        // ===== 카테고리 정책 =====
        sb.append("허용 카테고리: ").append(allowList).append("\n")
                .append("규칙: 최근 소비 카테고리가 존재하면 그 목록만 사용해 우선 생성하고, 마지막 1개는 창의적으로 허용 목록 내에서 선택.\n\n");

        // ===== 시간대 가이드 =====
        sb.append("피크 시간대 반영 가이드:\n")
                .append("- MORNING: 아침 루틴/모닝세트/등굣길·출근길/가벼운 소비\n")
                .append("- AFTERNOON: 점심·티타임/카공/짧은 산책·전시 관람\n")
                .append("- EVENING: 퇴근 후·저녁·데이트/디저트/책카페\n")
                .append("- NIGHT: 야식/늦은 카페/편의점 간식/야간 산책\n\n");

        // ===== 대체/변형 규칙(핵심) =====
        sb.append("소비패턴 ‘대체/변형’ 규칙: 동일 소비를 복제하지 말고, 아래처럼 바꿔 제안하세요.\n")
                .append("- 카페: 커피→차/주스/디카페인, 머그컵/텀블러, 로컬/시그니처, 시럽/토핑 변화\n")
                .append("- 식당: 고기→채식/샐러드, 1만원 컷, 타국가 음식, 1인분·저염/저칼로리\n")
                .append("- 편의점: 과자→견과류/프로틴바, 제로/무가당, 3000원 세트, 샐러드/국물류\n")
                .append("- 서점/문구점: 새 책→중고책, 다른 장르, 소액 문구, 독서 30분, 지출 기록 노트\n")
                .append("- 박물관/미술관: 감상평/스케치/굿즈 소액/30분 이상 체류/동행 관람\n")
                .append("- 공원: 산책 시간 늘리기/줍깅/계단/랜드마크 인증/시간대 변환\n")
                .append("- 기타: 로컬 가게/시장 탐방/매장 식사/독립영화/사진/음악/소비 기록\n\n");

        // ===== 예시 아이디어 풀(힌트) =====
        sb.append("아래는 카테고리별 변형 아이디어 예시(그대로 복사 금지, 참고만):\n");
        appendIdeaPoolSection(sb, topCatsStr);

        // ===== 생성 규칙(중복 금지 등) =====
        sb.append("\n생성 규칙:\n")
                .append("1) 총 4개: 최근 패턴 반영 3개 + 완전 창의적 1개.\n")
                .append("2) 각 미션은 서로 다른 테마/조건/어조. 동일 문구/패턴 금지.\n")
                .append("3) ‘title’은 재치 있고 직관적. mz스럽게 센스 있는 문구. 재밌는 문구로 사용자로 하여금 흥미 유발되도록. ‘AI 미션 N’ 같은 제목 금지.\n")
                .append("4) ‘description’에 ‘대신 ~하기/평소와 다르게 ~하기/새로운 ~도전’ 같은 변화를 명시.\n")
                .append("5) 금액/난이도에 따라 rewardPoint(50~300)를 다양화.\n")
                .append("6) 각 미션은 영수증으로 인증할 수 있는 형태.무조건! 중요!\n")
                .append("7) 영수증으로 인증할 수 없으면 안됨. 예를 들어 3줄 이상의 감상평, 30분 이상 산책 -> 영수증으로 인증할 수 없는 형태임. 안됨.\n")
                .append("8) 영수증 인증하면 추가 리워드 지급 등 멘트 금지. 미션 성공 -> 리워드 지급 이런 흐름인데 미션을 성공하려면 영수증 인증을 거쳐야 함.\n")
                .append("9) 그리고 '15000원 이상 구매시 영수증 인증' 이런 멘트보다는 '15000원 이상 구매 후 영수증 인증' 이렇게 확정지어서 말해줘야 함.\n")
                .append("10) 결과는 JSON 배열만 출력.\n\n");

        // ===== 최종 요청 =====
        sb.append("이제 위 규칙을 지켜 서로 다른 4개의 미션을 생성하세요. JSON 배열만 출력하세요.");

        String prompt = sb.toString();
        log.debug("[AI] Generated prompt:\n{}", prompt);
        return prompt;
    }

    /** 아이디어 풀을 프롬프트에 짧게 삽입 (상위 카테고리 우선, 없으면 전부) */
    private void appendIdeaPoolSection(StringBuilder sb, List<String> topCatsStr) {
        List<String> order = topCatsStr.isEmpty()
                ? new ArrayList<>(IDEA_POOL.keySet())
                : new ArrayList<>(topCatsStr);

        // 존재하지 않는 키가 있을 수 있으니 보정
        order = order.stream()
                .filter(IDEA_POOL::containsKey)
                .collect(Collectors.toList());

        // 상위 카테고리가 IDEA_POOL에 없으면 기본 순서도 넣기
        if (order.isEmpty()) order = new ArrayList<>(IDEA_POOL.keySet());

        for (String cat : order) {
            List<String> ideas = IDEA_POOL.getOrDefault(cat, Collections.emptyList());
            if (ideas.isEmpty()) continue;
            // 너무 길어지지 않게 상위 5~6개만 샘플 노출
            List<String> sample = ideas.subList(0, Math.min(6, ideas.size()));
            sb.append("- ").append(cat).append(": ").append(String.join(", ", sample)).append("\n");
        }
    }
}
