package com.example.hackathon.receipt.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class ReceiptOcrParser {

    private static final ObjectMapper M = new ObjectMapper();

    // ===== 숫자/금액 =====
    private static final Pattern AMOUNT = Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\s*원)?");
    private static final String[] TOTAL_HINTS_PRIORITY = {
            "받을금액", "받을 금액", "결제금액", "결제 금액", "총금액", "총 금액", "청구금액", "청구 금액",
            "총액", "총  액", "총 합계", "합계", "합  계", "합  계:"
    };
    private static final String[] TABLE_HINTS = {"메뉴", "단가", "수량", "금액"};

    // ===== 사업자/전화 =====
    private static final Pattern BIZNO = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{5}\\b");
    private static final Pattern PHONE = Pattern.compile("(TEL|Tel|전화|연락처|\\d{2,3}[)\\-]\\d{3,4}-\\d{4})");

    // ===== 날짜/시간 =====
    private static final Pattern DATE = Pattern.compile("\\b(19|20)\\d{2}[./-]\\d{1,2}[./-]\\d{1,2}\\b");
    private static final Pattern DATE_KR = Pattern.compile("\\b(19|20)\\d{2}년\\s*\\d{1,2}월\\s*\\d{1,2}일\\b");
    private static final Pattern TIME = Pattern.compile("\\b(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\b");
    private static final Pattern TIME_AP = Pattern.compile("\\b(오전|오후)\\s*(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\b");
    private static final Pattern BROKEN_TIME = Pattern.compile("(\\b\\d{1,2}):\\s*(\\d{2}\\b)");

    // ===== 주소 =====
    private static final String[] ADDR_TOKENS = {
            "특별시","광역시","특별자치시","특별자치도","도","시","군","구","읍","면","동","리","로","길"
    };
    private static final Map<String, String> SIDO_MAP = Map.ofEntries(
            Map.entry("서울", "서울특별시"),
            Map.entry("부산", "부산광역시"),
            Map.entry("대구", "대구광역시"),
            Map.entry("인천", "인천광역시"),
            Map.entry("광주", "광주광역시"),
            Map.entry("대전", "대전광역시"),
            Map.entry("울산", "울산광역시"),
            Map.entry("세종", "세종특별자치시"),
            Map.entry("경기", "경기도"),
            Map.entry("강원", "강원특별자치도"),
            Map.entry("충북", "충청북도"),
            Map.entry("충남", "충청남도"),
            Map.entry("전북", "전북특별자치도"),
            Map.entry("전남", "전라남도"),
            Map.entry("경북", "경상북도"),
            Map.entry("경남", "경상남도"),
            Map.entry("제주", "제주특별자치도")
    );
    private static final Set<String> SIDO_FULL = new HashSet<>(SIDO_MAP.values());

    // ===== 상호 키워드 =====
    private static final String[] STORE_HINTS = {"상호", "상호명", "가맹점", "매장명", "업체명", "상점명"};

    // ======= 상호명 기반 카테고리 오버라이드 =======
    // 1) 상호명 정확 매핑(최우선)
    // 2) 키워드 기반 휴리스틱(차선)
    // 카테고리 명은 서버의 표기와 맞추기(예: "식당", "카페", "편의점", "쇼핑센터" 등).

    private static final Map<String, String> CATEGORY_OVERRIDE_BY_STORE = new HashMap<>();
    static {
        // === 여기에 "정확 상호" → "원하는 카테고리"를 하드코딩 ===
        // placeCategory에 있는 장소 정확히 입력

        CATEGORY_OVERRIDE_BY_STORE.put("한돈당", "식당");
        CATEGORY_OVERRIDE_BY_STORE.put("스타벅스", "카페");
        CATEGORY_OVERRIDE_BY_STORE.put("이디야커피", "카페");
        // CATEGORY_OVERRIDE_BY_STORE.put("OO마트 송도점", "쇼핑센터");
    }

    /** 키워드 우선순위가 앞에 올수록 먼저 매칭됨(LinkedHashMap 권장) */
    private static final LinkedHashMap<String, String> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    static {
        // === 상호명에 포함되면 매칭할 키워드 → 카테고리(간단 휴리스틱) ===
        CATEGORY_KEYWORDS.put("카페", "카페");
        CATEGORY_KEYWORDS.put("커피", "카페");
        CATEGORY_KEYWORDS.put("커피숍", "카페");
        CATEGORY_KEYWORDS.put("베이커리", "카페");

        CATEGORY_KEYWORDS.put("식당", "식당");
        CATEGORY_KEYWORDS.put("한식", "식당");
        CATEGORY_KEYWORDS.put("분식", "식당");
        CATEGORY_KEYWORDS.put("김밥", "식당");
        CATEGORY_KEYWORDS.put("고기", "식당");
        CATEGORY_KEYWORDS.put("한돈", "식당");
        CATEGORY_KEYWORDS.put("장어", "식당");
        CATEGORY_KEYWORDS.put("초밥", "식당");
        CATEGORY_KEYWORDS.put("스시", "식당");
        CATEGORY_KEYWORDS.put("비비큐", "식당");
        CATEGORY_KEYWORDS.put("BBQ", "식당");

        CATEGORY_KEYWORDS.put("편의점", "편의점");
        CATEGORY_KEYWORDS.put("CU", "편의점");
        CATEGORY_KEYWORDS.put("GS25", "편의점");
        CATEGORY_KEYWORDS.put("세븐일레븐", "편의점");

        CATEGORY_KEYWORDS.put("마트", "쇼핑센터");
        CATEGORY_KEYWORDS.put("백화점", "쇼핑센터");
        CATEGORY_KEYWORDS.put("아울렛", "쇼핑센터");
        CATEGORY_KEYWORDS.put("문구", "쇼핑센터");
        CATEGORY_KEYWORDS.put("서점", "쇼핑센터");
    }

    /** 외부에서 사용: 규칙 결과(detected)가 수상할 때 상호명으로 교정 */
    public static String overrideCategoryByStoreName(String detectedPlaceCategory, String storeName) {
        String normalizedStore = normalize(storeName);

        // 1) 정확 상호 매핑(최우선)
        if (normalizedStore != null) {
            for (Map.Entry<String, String> e : CATEGORY_OVERRIDE_BY_STORE.entrySet()) {
                if (normalizedStore.equals(normalize(e.getKey()))) {
                    return e.getValue();
                }
            }
        }

        // 2) 키워드 매칭(차선)
        String keywordGuess = guessCategoryByKeyword(normalizedStore);
        if (keywordGuess != null) return keywordGuess;

        // 3) 오버라이드 못하면 원래 값 유지
        return detectedPlaceCategory;
    }


    public static String forceCategory(String preferredCategory, String detectedPlaceCategory) {
        return (preferredCategory != null && !preferredCategory.isBlank())
                ? preferredCategory
                : detectedPlaceCategory;
    }

    private static String guessCategoryByKeyword(String normalizedStore) {
        if (normalizedStore == null || normalizedStore.isBlank()) return null;
        for (Map.Entry<String, String> e : CATEGORY_KEYWORDS.entrySet()) {
            if (normalizedStore.contains(normalize(e.getKey()))) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String normalize(String s) {
        if (s == null) return null;
        return s.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }


    public static ReceiptParsed parse(String clovaJson) throws Exception {
        JsonNode root = M.readTree(clovaJson);
        JsonNode images = root.path("images");
        if (!images.isArray() || images.size() == 0) {
            return new ReceiptParsed(null, null, null, null, null, null, null);
        }
        JsonNode fields = images.get(0).path("fields");

        // 1) 라인 복원 + 정규화
        List<String> lines = rebuildLines(fields);
        String text = String.join("\n", lines);
        text = BROKEN_TIME.matcher(text).replaceAll("$1:$2");

        // 2) 상호명 추출(강화)
        String storeName = extractStoreName(lines);

        // 3) 주소 전체 라인 추출(강화)
        String storeAddressFull = extractAddressFull(lines);

        // 3-1) 주소 분해 + 시도 축약 확장 + 시군구 보정
        String[] parts = splitKoreanAddressRobust(storeAddressFull);
        String sido  = parts[0];
        String gugun = parts[1];
        String dong  = parts[2];

        // 4) 결제 일시(강화: KR 포맷/오전·오후/라인 분리 대응)
        LocalDateTime paidAt = extractPaidAtStrong(text, lines);

        // 5) 합계 금액(강화: 우선순위 키워드 + 테이블 라인 제외 + 전역 폴백)
        BigDecimal totalAmount = extractTotalAmountStrong(lines);

        return new ReceiptParsed(
                emptyToNull(storeName),
                emptyToNull(storeAddressFull),
                emptyToNull(sido),
                emptyToNull(gugun),
                emptyToNull(dong),
                paidAt,
                totalAmount
        );
    }

    // ================= 내부 유틸 =================

    private static List<String> rebuildLines(JsonNode fields) {
        List<String> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (JsonNode f : fields) {
            String t = f.path("inferText").asText("");
            boolean br = f.path("lineBreak").asBoolean(false);

            if (!t.isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(t.trim());
            }
            if (br) {
                String line = cleanNoise(sb.toString());
                if (!line.isBlank()) lines.add(line);
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) {
            String line = cleanNoise(sb.toString());
            if (!line.isBlank()) lines.add(line);
        }
        return lines;
    }

    private static String cleanNoise(String s) {
        if (s == null) return "";
        String t = s;
        t = t.replaceAll("[\\[\\]{}()]+", " ");
        t = t.replaceAll("[：:]+", ":");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }

    // ---------- 상호명 ----------
    private static String extractStoreName(List<String> lines) {
        // 1) 사업자번호 주변 우선
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            if (BIZNO.matcher(ln).find()) {
                String up = (i > 0) ? lines.get(i - 1) : null;
                String down = (i + 1 < lines.size()) ? lines.get(i + 1) : null;
                String cand = preferStoreCandidate(up, down);
                if (cand != null) return clean(cand);
            }
        }
        // 2) 상호 키워드 포함 라인
        for (String line : lines) {
            for (String h : STORE_HINTS) {
                if (line.contains(h)) {
                    String s = line.replaceAll(".*" + h, "").replaceAll("[:\\-]\\s*", "").trim();
                    if (!s.isBlank() && !looksLikePhoneOrAddressOrAmount(s)) return clean(s);
                }
            }
        }
        // 3) 상단 타이틀(첫 3줄) 중 주소/전화/금액 아닌 라인
        int limit = Math.min(3, lines.size());
        for (int i = 0; i < limit; i++) {
            String line = lines.get(i);
            if (!looksLikePhoneOrAddressOrAmount(line)) return clean(line);
        }
        return lines.isEmpty() ? null : clean(lines.get(0));
    }

    private static String preferStoreCandidate(String a, String b) {
        String ca = pickStoreFrom(a);
        String cb = pickStoreFrom(b);
        if (ca != null && cb != null) {
            boolean aHas = containsAny(a, STORE_HINTS);
            boolean bHas = containsAny(b, STORE_HINTS);
            if (aHas && !bHas) return ca;
            if (bHas && !aHas) return cb;
            return (ca.length() <= cb.length()) ? ca : cb;
        }
        return (ca != null) ? ca : cb;
    }

    private static String pickStoreFrom(String line) {
        if (line == null) return null;
        if (looksLikePhoneOrAddressOrAmount(line)) return null;
        for (String h : STORE_HINTS) {
            if (line.contains(h)) {
                String s = line.replaceAll(".*" + h, "").replaceAll("[:\\-]\\s*", "").trim();
                if (!s.isBlank()) return s;
            }
        }
        return line.trim();
    }

    private static boolean looksLikePhoneOrAddressOrAmount(String s) {
        if (s == null) return false;
        if (AMOUNT.matcher(s).find()) return true;
        if (PHONE.matcher(s).find()) return true;
        int score = 0;
        for (String t : ADDR_TOKENS) if (s.contains(t)) score++;
        return score >= 2;
    }

    // ---------- 주소 ----------
    private static String extractAddressFull(List<String> lines) {
        String best = null;
        int bestScore = Integer.MIN_VALUE;

        for (int idx = 0; idx < lines.size(); idx++) {
            String line = lines.get(idx);
            int score = 0;

            for (String token : ADDR_TOKENS) if (line.contains(token)) score += 2;

            int len = line.length();
            if (len >= 10) score += 1;
            if (line.matches(".*\\d{1,4}(-\\d{1,4})?.*")) score += 1;

            if (PHONE.matcher(line).find()) score -= 3;
            if (AMOUNT.matcher(line).find()) score -= 2;
            if (containsAny(line, TABLE_HINTS)) score -= 2;

            if (idx > 0 && containsAny(lines.get(idx - 1), ADDR_TOKENS)) score += 1;
            if (idx + 1 < lines.size() && containsAny(lines.get(idx + 1), ADDR_TOKENS)) score += 1;

            if (score > bestScore) { bestScore = score; best = line; }
        }
        return best == null ? null : clean(best);
    }

    /** 축약형 시도 확장 + 시/군/구 보정 포함 */
    private static String[] splitKoreanAddressRobust(String full) {
        String sido = null, gugun = null, dong = null;
        if (full == null || full.isBlank()) return new String[]{null, null, null};

        String[] toks = full.split("\\s+");
        for (int i = 0; i < toks.length; i++) {
            String tok = toks[i];

            if (sido == null) {
                String cand = expandSido(tok);
                if (isSido(cand) || tok.endsWith("특별자치도") || tok.endsWith("특별자치시")
                        || tok.endsWith("특별시") || tok.endsWith("광역시") || tok.endsWith("도")) {
                    sido = isSido(cand) ? cand : tok;
                    continue;
                }
            }
            if (gugun == null && (tok.endsWith("시") || tok.endsWith("군") || tok.endsWith("구"))) {
                gugun = tok;
                continue;
            }
            if (dong == null && (tok.endsWith("읍") || tok.endsWith("면") || tok.endsWith("동"))) {
                dong = tok;
            }
        }

        if (sido != null) sido = expandSido(sido);
        if (sido == null && toks.length > 0 && isSido(expandSido(toks[0]))) {
            sido = expandSido(toks[0]);
            if (gugun == null && toks.length > 1 && toks[1].matches(".+(시|군|구)$")) gugun = toks[1];
            if (dong == null && toks.length > 2 && toks[2].matches(".+(읍|면|동)$")) dong = toks[2];
        }

        return new String[]{sido, gugun, dong};
    }

    private static String expandSido(String token) {
        if (token == null) return null;
        if (SIDO_FULL.contains(token)) return token;
        String base = token.replaceAll("(특별자치시|특별자치도|특별시|광역시|도)$", "");
        return SIDO_MAP.getOrDefault(base, token);
    }

    private static boolean isSido(String s) {
        return s != null && (SIDO_FULL.contains(s) || s.endsWith("도") || s.endsWith("특별시")
                || s.endsWith("광역시") || s.endsWith("특별자치도") || s.endsWith("특별자치시"));
    }

    // ---------- 날짜/시간 ----------
    private static LocalDateTime extractPaidAtStrong(String text, List<String> lines) {
        LocalDate d = findDate(text);
        LocalTime t = findTime(text);
        if (d != null && t != null) return LocalDateTime.of(d, t);
        if (d != null) return LocalDateTime.of(d, LocalTime.MIDNIGHT);

        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            LocalDate dl = findDate(ln);
            if (dl != null) {
                for (int j = Math.max(0, i - 2); j <= Math.min(lines.size() - 1, i + 2); j++) {
                    LocalTime tl = findTime(lines.get(j));
                    if (tl != null) return LocalDateTime.of(dl, tl);
                }
                return LocalDateTime.of(dl, LocalTime.MIDNIGHT);
            }
        }
        return null;
    }

    private static LocalDate findDate(String s) {
        if (s == null) return null;
        Matcher m1 = DATE.matcher(s);
        if (m1.find()) {
            String[] p = m1.group().split("[./-]");
            int y = Integer.parseInt(p[0]);
            int mo = Integer.parseInt(p[1]);
            int d = Integer.parseInt(p[2]);
            return safeDate(y, mo, d);
        }
        Matcher m2 = DATE_KR.matcher(s);
        if (m2.find()) {
            String g = m2.group().replaceAll("[^0-9]", " ").trim().replaceAll("\\s+", " ");
            String[] p = g.split(" ");
            if (p.length >= 3) {
                int y = Integer.parseInt(p[0]);
                int mo = Integer.parseInt(p[1]);
                int d = Integer.parseInt(p[2]);
                return safeDate(y, mo, d);
            }
        }
        return null;
    }

    private static LocalTime findTime(String s) {
        if (s == null) return null;
        Matcher ap = TIME_AP.matcher(s);
        if (ap.find()) {
            boolean pm = ap.group(1).contains("오후");
            int hh = Integer.parseInt(ap.group(2));
            int mm = Integer.parseInt(ap.group(3));
            int ss = (ap.group(4) != null) ? Integer.parseInt(ap.group(4)) : 0;
            if (pm && hh < 12) hh += 12;
            if (!pm && hh == 12) hh = 0;
            return safeTime(hh, mm, ss);
        }
        Matcher t = TIME.matcher(s);
        if (t.find()) {
            int hh = Integer.parseInt(t.group(1));
            int mm = Integer.parseInt(t.group(2));
            int ss = (t.group(3) != null) ? Integer.parseInt(t.group(3)) : 0;
            return safeTime(hh, mm, ss);
        }
        return null;
    }

    private static LocalDate safeDate(int y, int m, int d) {
        try { return LocalDate.of(y, m, d); } catch (Exception e) { return null; }
    }
    private static LocalTime safeTime(int h, int m, int s) {
        try { return LocalTime.of(h, m, s); } catch (Exception e) { return null; }
    }

    // ---------- 금액 ----------
    private static BigDecimal extractTotalAmountStrong(List<String> lines) {
        int tableHeaderIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (containsAny(lines.get(i), TABLE_HINTS)) { tableHeaderIdx = i; break; }
        }

        BigDecimal best = null;
        for (int pri = 0; pri < TOTAL_HINTS_PRIORITY.length; pri++) {
            String hint = TOTAL_HINTS_PRIORITY[pri];
            for (int i = lines.size() - 1; i >= 0; i--) {
                String ln = lines.get(i);
                if (!ln.contains(hint)) continue;
                if (tableHeaderIdx >= 0 && i > tableHeaderIdx + 1) {
                    if (!(hint.contains("받을") || hint.contains("결제") || hint.contains("총금"))) {
                        continue;
                    }
                }
                BigDecimal v = maxAmountIn(ln);
                if (v != null) return v;
            }
        }

        for (int i = lines.size() - 1; i >= 0; i--) {
            String ln = lines.get(i);
            if (tableHeaderIdx >= 0 && i > tableHeaderIdx) {
                if (ln.matches(".*\\b(\\d+[,\\d]*)\\b\\s*$")) continue;
            }
            BigDecimal v = maxAmountIn(ln);
            if (v != null) {
                if (best == null || v.compareTo(best) > 0) best = v;
            }
        }
        return best;
    }

    private static BigDecimal maxAmountIn(String s) {
        Matcher m = AMOUNT.matcher(s);
        BigDecimal best = null;
        while (m.find()) {
            String raw = m.group(1);
            BigDecimal v = toBigDecimal(raw);
            if (v != null) {
                if (best == null || v.compareTo(best) > 0) best = v;
            }
        }
        return best;
    }

    private static BigDecimal toBigDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.replaceAll("[^0-9.]", "");
        if (clean.isBlank()) return null;
        try { return new BigDecimal(clean); } catch (NumberFormatException e) { return null; }
    }

    // ---------- 공통 ----------
    private static boolean containsAny(String s, String[] keys) {
        if (s == null) return false;
        for (String k : keys) { if (s.contains(k)) return true; }
        return false;
    }

    private static String clean(String s) {
        if (s == null) return null;
        return s.replaceAll("^[\\[\\]\\s:]+", "")
                .replaceAll("[\\[\\]\\s:]+$", "")
                .trim();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
