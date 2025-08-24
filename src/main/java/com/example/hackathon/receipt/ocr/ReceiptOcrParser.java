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
    private static final Pattern AMOUNT = Pattern.compile("([0-9]{1,3}(?:[,\\.][0-9]{3})+|[0-9]+)(?:\\s*원)?");
    private static final String[] AMOUNT_BANNED_CONTEXT = {
            "사업자","사업자번호","등록번호","영수증NO","영수증 NO","거래번호","승인번호","승인 No","카드","카드번호",
            "고객센터","주문번호","우편번호","바코드","POS","포스","TEL","Tel","전화","휴대폰","핸드폰","계좌","계산서",
            "가맹점번호","단말기","전표","매입사","발급사","카드사","매출전표","할인","할인액","쿠폰"
    };
    private static final long AMOUNT_MIN = 100L;
    private static final long AMOUNT_MAX = 10_000_000L;

    private static final String[] TOTAL_HINTS_PRIORITY = {
            "받을금액","받을 금액","받은금액","받은 금액",
            "결제금액","결제 금액","카드청구액","카드 청구액",
            "총금액","총 금액","청구금액","청구 금액",
            "총액","총  액","총 합계","합계","합  계","합  계:",
            "소계","소 계","소  계"
    };
    private static final String[] TABLE_HINTS = {"품명","메뉴","단가","수량","금액"};

    // ===== 사업자/전화 =====
    private static final Pattern BIZNO_HYPHEN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{5}\\b");
    private static final Pattern BIZNO_PURE10 = Pattern.compile("\\b\\d{10}\\b");
    private static final Pattern PHONE = Pattern.compile("(TEL|Tel|전화|연락처|\\d{2,3}[)\\-]\\d{3,4}-\\d{4})");

    // ===== 날짜/시간 =====
    private static final Pattern DATE = Pattern.compile("\\b(19|20)\\d{2}[./-]\\d{1,2}[./-]\\d{1,2}\\b");
    private static final Pattern DATE_KR = Pattern.compile("\\b(19|20)\\d{2}년\\s*\\d{1,2}월\\s*\\d{1,2}일\\b");
    private static final Pattern TIME = Pattern.compile("\\b(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\b");
    private static final Pattern TIME_AP = Pattern.compile("\\b(오전|오후)\\s*(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\b");
    private static final Pattern BROKEN_TIME = Pattern.compile("(\\b\\d{1,2}):\\s*(\\d{2}\\b)");
    private static final Pattern YEAR_LIKE = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern DATE_WORD = Pattern.compile("(일자|년|월|일)");

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

    // ★ 구만 나올 때 시/도 보정
    private static final Set<String> SEOUL_GU = new HashSet<>(Arrays.asList(
            "강남구","강동구","강북구","강서구","관악구","광진구","구로구","금천구","노원구","도봉구","동대문구",
            "동작구","마포구","서대문구","서초구","성동구","성북구","송파구","양천구","영등포구","용산구",
            "은평구","종로구","중구","중랑구"
    ));

    // ===== 상호 키워드/정제 =====
    private static final String[] STORE_HINTS = {"상호","상호명","가맹점","매장명","업체명","상점명"};
    private static final String[] STORE_STOPWORDS = { // ★ 상단 제목 제거
            "영수증","[영 수 증]","영 수 증","[영수증]","매출전표","신용승인정보","고객용","가맹점용"
    };
    private static final Pattern BRANCH_POINT = Pattern.compile(".+점\\)?" ); // ★ ○○점 포함 라인 선호

    // ======= 상호명 기반 카테고리 오버라이드 =======
    private static final Map<String, String> CATEGORY_OVERRIDE_BY_STORE = new HashMap<>();
    static {
        CATEGORY_OVERRIDE_BY_STORE.put("한돈당", "식당");
        CATEGORY_OVERRIDE_BY_STORE.put("스타벅스", "카페");
        CATEGORY_OVERRIDE_BY_STORE.put("이디야커피", "카페");
        CATEGORY_OVERRIDE_BY_STORE.put("아워스 OURS", "카페");
        CATEGORY_OVERRIDE_BY_STORE.put("OURS", "카페");
        CATEGORY_OVERRIDE_BY_STORE.put("아워스", "카페");
    }
    private static final LinkedHashMap<String, String> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    static {
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

    public static String overrideCategoryByStoreName(String detectedPlaceCategory, String storeName) {
        String normalizedStore = normalize(storeName);
        if (normalizedStore != null) {
            for (Map.Entry<String, String> e : CATEGORY_OVERRIDE_BY_STORE.entrySet()) {
                if (normalizedStore.equals(normalize(e.getKey()))) {
                    return e.getValue();
                }
            }
        }
        String keywordGuess = guessCategoryByKeyword(normalizedStore);
        if (keywordGuess != null) return keywordGuess;
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

    // ===== 파서 =====
    public static ReceiptParsed parse(String clovaJson) throws Exception {
        JsonNode root = M.readTree(clovaJson);
        JsonNode images = root.path("images");
        if (!images.isArray() || images.size() == 0) {
            return new ReceiptParsed(null, null, null, null, null, null, null);
        }
        JsonNode fields = images.get(0).path("fields");

        List<String> lines = rebuildLines(fields);
        String text = String.join("\n", lines);
        text = BROKEN_TIME.matcher(text).replaceAll("$1:$2");

        String storeName = extractStoreName(lines);               // 강화
        String storeAddressFull = extractAddressFull(lines);
        String[] parts = splitKoreanAddressRobust(storeAddressFull, lines); // 괄호 동 탐색
        String sido  = parts[0];
        String gugun = parts[1];
        String dong  = parts[2];

        LocalDateTime paidAt = extractPaidAtStrong(text, lines);
        BigDecimal totalAmount = extractTotalAmountStrong(lines); // 금액 오인 방지 강화

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
        t = t.replaceAll("^\\s*주소\\s*[:：]\\s*", ""); // "주소 :" 접두 제거
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }

    // ---------- 상호명 (보강) ----------
    private static String extractStoreName(List<String> lines) {
        // 0) 상단 STOPWORD 라인 제거(영수증 타이틀 등)
        List<String> filtered = new ArrayList<>();
        for (String ln : lines) {
            boolean stop = false;
            for (String sw : STORE_STOPWORDS) if (ln.contains(sw)) { stop = true; break; }
            if (!stop) filtered.add(ln);
        }
        if (filtered.isEmpty()) filtered = lines;

        // 1) 사업자번호 주변 우선
        for (int i = 0; i < filtered.size(); i++) {
            String ln = filtered.get(i);
            if (BIZNO_HYPHEN.matcher(ln).find() || BIZNO_PURE10.matcher(ln).find()) {
                String up = (i > 0) ? filtered.get(i - 1) : null;
                String down = (i + 1 < filtered.size()) ? filtered.get(i + 1) : null;
                String cand = preferStoreCandidate(up, down);
                if (cand != null) return cleanStoreName(cand);
            }
        }
        // 2) '○○점' 포함 라인 선호(예: 카페아이엔티(여의도KBS점) / …)
        for (String ln : filtered) {
            if (BRANCH_POINT.matcher(ln).find() && !looksLikePhoneOrAddressOrAmount(ln)) {
                return cleanStoreName(ln);
            }
        }
        // 3) 상호 키워드 포함 라인
        for (String line : filtered) {
            for (String h : STORE_HINTS) {
                if (line.contains(h)) {
                    String s = line.replaceAll(".*" + h, "").replaceAll("[:\\-]\\s*", "").trim();
                    if (!s.isBlank() && !looksLikePhoneOrAddressOrAmount(s)) return cleanStoreName(s);
                }
            }
        }
        // 4) 상단 타이틀(첫 3줄)
        int limit = Math.min(3, filtered.size());
        for (int i = 0; i < limit; i++) {
            String line = filtered.get(i);
            if (!looksLikePhoneOrAddressOrAmount(line)) return cleanStoreName(line);
        }
        return filtered.isEmpty() ? null : cleanStoreName(filtered.get(0));
    }
    private static String cleanStoreName(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // 슬래시로 이어진 경우 첫 구간만(뒤는 지점/부가정보일 가능성 높음)
        if (s.contains("/")) s = s.split("\\s*/\\s*")[0];

        // 주소/전화/금액 성격 제거
        if (looksLikePhoneOrAddressOrAmount(s)) return null;

        // 불필요 접두/접미 기호 정리
        s = s.replaceAll("^[\\-:·~•]+\\s*", "").replaceAll("\\s*[\\-:·~•]+$", "");

        // 괄호 속 지점명은 유지(예: 카페아이엔티(여의도KBS점))
        return clean(s);
    }
    private static String preferStoreCandidate(String a, String b) {
        String ca = pickStoreFrom(a);
        String cb = pickStoreFrom(b);
        if (ca != null && cb != null) {
            boolean aHas = containsAny(a, STORE_HINTS);
            boolean bHas = containsAny(b, STORE_HINTS);
            if (aHas && !bHas) return ca;
            if (bHas && !aHas) return cb;
            // '점' 포함한 쪽을 더 선호
            boolean aPoint = a.contains("점");
            boolean bPoint = b.contains("점");
            if (aPoint && !bPoint) return ca;
            if (bPoint && !aPoint) return cb;
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

    // 괄호 안 동명/이웃 줄까지 함께 탐색
    private static String[] splitKoreanAddressRobust(String full, List<String> lines) {
        String sido = null, gugun = null, dong = null;
        if (full == null || full.isBlank()) return new String[]{null, null, null};

        // 괄호에서 동명 우선 추출: (여의도동, …)
        Matcher parenDong = Pattern.compile("\\(([^)]*?)([\\w가-힣]+동)\\b[^)]*\\)").matcher(full);
        if (parenDong.find()) {
            dong = parenDong.group(2);
        }

        String[] toks = full.split("\\s+");
        for (int i = 0; i < toks.length; i++) {
            String tok = toks[i];
            // 시/도
            if (sido == null) {
                String cand = expandSido(tok);
                if (isSido(cand) || tok.endsWith("특별자치도") || tok.endsWith("특별자치시")
                        || tok.endsWith("특별시") || tok.endsWith("광역시") || tok.endsWith("도")) {
                    sido = isSido(cand) ? cand : tok;
                    continue;
                }
            }
            // 시/군/구
            if (gugun == null && (tok.endsWith("시") || tok.endsWith("군") || tok.endsWith("구"))) {
                gugun = tok; continue;
            }
            // 동/읍/면 + 변형
            if (dong == null) {
                if (tok.matches(".+동\\d*가?") || tok.matches(".+(읍|면)\\d*")) {
                    dong = tok;
                }
            }
        }

        // 동이 여전히 없으면: 전체 라인/이웃줄에서 재탐색
        if (dong == null) {
            Matcher m1 = Pattern.compile("(\\S+동\\d*가?)").matcher(full);
            if (m1.find()) dong = m1.group(1);
            if (dong == null) {
                for (String ln : lines) {
                    Matcher m2 = Pattern.compile("(\\S+동\\d*가?)").matcher(ln);
                    if (m2.find()) { dong = m2.group(1); break; }
                }
            }
        }

        // 구만 있는 경우 서울 시/도 보정
        if (sido == null && gugun != null && SEOUL_GU.contains(gugun)) {
            sido = "서울특별시";
        }

        if (sido != null) sido = expandSido(sido);
        if (sido == null && toks.length > 0 && isSido(expandSido(toks[0]))) {
            sido = expandSido(toks[0]);
            if (gugun == null && toks.length > 1 && toks[1].matches(".+(시|군|구)$")) gugun = toks[1];
            if (dong == null && toks.length > 2 && toks[2].matches(".+(읍|면|동)\\d*가?$")) dong = toks[2];
        }
        return new String[]{sido, gugun, dong};
    }

    // 기존 시그니처 유지용 오버로드
    private static String[] splitKoreanAddressRobust(String full) { return splitKoreanAddressRobust(full, Collections.emptyList()); }

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

        // 1) 힌트 라인 + 인접 라인
        for (String hint : TOTAL_HINTS_PRIORITY) {
            for (int i = lines.size() - 1; i >= 0; i--) {
                String ln = lines.get(i);
                if (!ln.contains(hint)) continue;

                if (tableHeaderIdx >= 0 && i > tableHeaderIdx + 1) {
                    if (!(hint.contains("받을") || hint.contains("받은") || hint.contains("결제")
                            || hint.contains("총금") || hint.contains("합계") || hint.contains("총액"))) {
                        continue;
                    }
                }
                BigDecimal v = bestAmountCandidate(ln);
                if (v != null) return v;
                if (i + 1 < lines.size()) {
                    v = bestAmountCandidate(lines.get(i + 1));
                    if (v != null) return v;
                }
                if (i - 1 >= 0) {
                    v = bestAmountCandidate(lines.get(i - 1));
                    if (v != null) return v;
                }
            }
        }

        // 2) 전역 폴백: 아래→위 합리적 최댓값
        BigDecimal best = null;
        for (int i = lines.size() - 1; i >= 0; i--) {
            String ln = lines.get(i);
            // 날짜/시간/연도 들어간 줄은 힌트 없으면 스킵(2018 오인 방지)
            if ((DATE.matcher(ln).find() || DATE_KR.matcher(ln).find() || TIME.matcher(ln).find() || YEAR_LIKE.matcher(ln).find())
                    && !containsAny(ln, TOTAL_HINTS_PRIORITY)) {
                continue;
            }
            if (tableHeaderIdx >= 0 && i > tableHeaderIdx) {
                if (ln.matches(".*\\b(\\d+[,\\d\\.]*)\\b\\s*$")) continue;
            }
            BigDecimal v = bestAmountCandidate(ln);
            if (v != null && (best == null || v.compareTo(best) > 0)) best = v;
        }
        return best;
    }
    private static BigDecimal bestAmountCandidate(String line) {
        if (line == null || line.isBlank()) return null;
        for (String bad : AMOUNT_BANNED_CONTEXT) if (line.contains(bad)) return null;
        if (BIZNO_HYPHEN.matcher(line).find() || BIZNO_PURE10.matcher(line).find()) return null;
        if ((DATE.matcher(line).find() || DATE_KR.matcher(line).find() || YEAR_LIKE.matcher(line).find() || DATE_WORD.matcher(line).find())
                && !containsAny(line, TOTAL_HINTS_PRIORITY)) return null;

        Matcher m = AMOUNT.matcher(line);
        BigDecimal best = null;
        while (m.find()) {
            String raw = m.group(1);
            BigDecimal v = toBigDecimal(raw);
            if (v == null) continue;
            long won = v.longValue();
            if (won < AMOUNT_MIN || won > AMOUNT_MAX) continue;
            int start = m.start(1);
            if (start > 0 && line.charAt(start - 1) == '-') continue; // 할인 음수 배제
            if (best == null || v.compareTo(best) > 0) best = v;
        }
        return best;
    }
    private static BigDecimal toBigDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.replaceAll("[^0-9]", "");
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
