package com.example.hackathon.receipt.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// CLOVA OCR(JSON) -> 우리가 필요한 4개 값(업체명/주소/결제일시/합계금액)으로 파싱


public class ReceiptOcrParser {

    private static final ObjectMapper M = new ObjectMapper();

    // 금액: "217,000", "12300" 등
    private static final Pattern AMOUNT = Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)");
    // 사업자등록번호: "610-09-74049" 같은 형태 (상호 추정 힌트로 사용)
    private static final Pattern BIZNO = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{5}\\b");

    // 날짜: 2013/09/22, 2013-09-22, 2013.09.22 모두 허용
    private static final Pattern DATE = Pattern.compile("\\b(19|20)\\d{2}[./-]\\d{1,2}[./-]\\d{1,2}\\b");
    // 시간: 15:56 또는 15:56:07
    private static final Pattern TIME = Pattern.compile("\\b(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\b");
    // 줄 나뉜 "15:" + 다음 줄 "56" -> "15:56" 로 합치기 위한 전처리
    private static final Pattern BROKEN_TIME = Pattern.compile("(\\b\\d{1,2}):\\s*(\\d{2}\\b)");

    // 주소 후보 키워드
    private static final String[] ADDR_TOKENS = {"도", "시", "군", "구", "읍", "면", "동", "리", "로", "길"};

    // 합계/총액 키워드(여러 영수증 포맷 커버)
    private static final String[] TOTAL_HINTS = {
            "합계", "합  계", "합  계:", "총액", "총  액", "받을금액", "받을 금액", "총 합계", "결제금액", "결제 금액"
    };

    public static ReceiptParsed parse(String clovaJson) throws Exception {
        // 0) JSON 파싱
        JsonNode root = M.readTree(clovaJson);
        JsonNode fields = root.path("images").get(0).path("fields");

        // 1) 토큰을 줄 단위 문자열로 복원
        List<String> lines = rebuildLines(fields);
        // 전체 문자열(줄바꿈 포함). "15:\n56" -> "15:56" 같은 시간 깨짐 보정
        String text = String.join("\n", lines);
        text = BROKEN_TIME.matcher(text).replaceAll("$1:$2");

        // 2) 상호(업체명) 추정: 사업자번호 라인 바로 위, 없으면 첫 줄
        String storeName = extractStoreName(lines);

        // 3) 주소 추정: 주소 토큰(시/구/동/로/길 등) 많이 포함된 라인 선택
        String storeAddress = extractAddress(lines);

        // 4) 결제 일시: 날짜+시간(또는 날짜만) 조합
        LocalDateTime paidAt = extractPaidAt(text);

        // 5) 합계 금액: 합계/받을금액/총액 등의 키워드 근처에서 최대 금액 찾기 (fallback: 전체에서 최댓값)
        BigDecimal totalAmount = extractTotalAmount(lines);

        return new ReceiptParsed(
                emptyToNull(storeName),
                emptyToNull(storeAddress),
                paidAt,
                totalAmount
        );
    }

    // 이 밑으로는 카테고리에 대한 적합 퍼센트 부분 (백엔드 확인용, 프론트에게 전달X)

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
                lines.add(sb.toString());
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) lines.add(sb.toString());
        return lines;
    }

    private static String extractStoreName(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (BIZNO.matcher(lines.get(i)).find()) {
                if (i > 0) return clean(lines.get(i - 1));
            }
        }
        // fallback: 첫 줄이 가게명인 경우가 많음
        return lines.isEmpty() ? null : clean(lines.get(0));
    }

    private static String extractAddress(List<String> lines) {
        // 주소스럽게 보이는 라인을 점수로 뽑기
        String best = null;
        int bestScore = 0;

        for (String line : lines) {
            int score = 0;
            for (String token : ADDR_TOKENS) {
                if (line.contains(token)) score++;
            }
            // 전화/금액/메뉴 같은 라인은 패스 가중치
            if (line.contains("TEL") || line.contains("전화") || AMOUNT.matcher(line).find()) score -= 2;
            if (score > bestScore) {
                bestScore = score;
                best = line;
            }
        }
        return best == null ? null : clean(best);
    }

    private static LocalDateTime extractPaidAt(String text) {
        // 날짜 1개, 시간 1개를 전역에서 찾아 합치기
        Matcher dm = DATE.matcher(text);
        Matcher tm = TIME.matcher(text);

        String dateStr = dm.find() ? dm.group() : null;
        String timeStr = tm.find() ? tm.group() : null;

        if (dateStr == null && timeStr == null) return null;

        String isoDate = normalizeDate(dateStr); // yyyy-MM-dd
        String isoTime = normalizeTime(timeStr); // HH:mm:ss

        try {
            if (isoDate != null && isoTime != null) {
                return LocalDateTime.parse(isoDate + "T" + isoTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else if (isoDate != null) {
                return LocalDateTime.parse(isoDate + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                // 날짜가 없고 시간만 있으면 못 씀 → null
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal extractTotalAmount(List<String> lines) {
        // 1) 합계/총액 힌트가 있는 라인들에서 가장 큰 금액을 우선 추출
        BigDecimal best = null;

        for (String line : lines) {
            if (!containsAny(line, TOTAL_HINTS)) continue;
            BigDecimal maxInLine = maxAmountIn(line);
            if (maxInLine != null) {
                if (best == null || maxInLine.compareTo(best) > 0) {
                    best = maxInLine;
                }
            }
        }
        if (best != null) return best;

        // 2) 힌트가 없다면 전체 라인에서 최댓값을 고름(메뉴 합계보다 "받을금액"이 보통 더 큼)
        for (String line : lines) {
            BigDecimal maxInLine = maxAmountIn(line);
            if (maxInLine != null) {
                if (best == null || maxInLine.compareTo(best) > 0) {
                    best = maxInLine;
                }
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

    private static String normalizeDate(String d) {
        if (d == null) return null;
        String[] parts = d.split("[./-]");
        if (parts.length != 3) return null;
        String yyyy = parts[0];
        String mm = pad2(parts[1]);
        String dd = pad2(parts[2]);
        return yyyy + "-" + mm + "-" + dd;
    }

    private static String normalizeTime(String t) {
        if (t == null) return null;
        // HH:mm[:ss] -> HH:mm:ss
        String[] parts = t.split(":");
        if (parts.length < 2) return null;
        String HH = pad2(parts[0]);
        String mm = pad2(parts[1]);
        String ss = (parts.length >= 3) ? pad2(parts[2]) : "00";
        return HH + ":" + mm + ":" + ss;
    }

    private static String pad2(String s) {
        if (s == null) return null;
        s = s.trim();
        return (s.length() == 1) ? "0" + s : s;
    }

    private static BigDecimal toBigDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.replaceAll("[^0-9.]", "");
        if (clean.isBlank()) return null;
        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean containsAny(String s, String[] keys) {
        for (String k : keys) {
            if (s.contains(k)) return true;
        }
        return false;
    }

    private static String clean(String s) {
        if (s == null) return null;
        // 앞뒤 대괄호/콜론/공백 등 자잘한 노이즈 제거
        return s.replaceAll("^[\\[\\]\\s:]+", "")
                .replaceAll("[\\[\\]\\s:]+$", "")
                .trim();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
