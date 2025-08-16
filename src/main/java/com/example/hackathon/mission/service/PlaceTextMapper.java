package com.example.hackathon.mission.service;

import com.example.hackathon.mission.entity.PlaceCategory;

import java.util.*;
import java.util.stream.Collectors;

public class PlaceTextMapper {
    private static final Map<String, PlaceCategory> KMAP;

    static {
        Map<String, PlaceCategory> m = new LinkedHashMap<>();
        m.put("카페", PlaceCategory.CAFE);
        m.put("식당", PlaceCategory.RESTAURANT);
        m.put("박물관/미술관", PlaceCategory.MUSEUM);
        m.put("박물관", PlaceCategory.MUSEUM);
        m.put("미술관", PlaceCategory.MUSEUM);
        m.put("도서관", PlaceCategory.LIBRARY);
        m.put("공원/산책로", PlaceCategory.PARK);
        m.put("공원", PlaceCategory.PARK);
        m.put("산책로", PlaceCategory.PARK);
        m.put("운동 시설", PlaceCategory.SPORTS_FACILITY);
        m.put("운동시설", PlaceCategory.SPORTS_FACILITY);
        m.put("쇼핑센터", PlaceCategory.SHOPPING_MALL);
        m.put("쇼핑 센터", PlaceCategory.SHOPPING_MALL);
        m.put("전통 시장", PlaceCategory.TRADITIONAL_MARKET);
        m.put("전통시장", PlaceCategory.TRADITIONAL_MARKET);
        m.put("기타", PlaceCategory.OTHER);
        KMAP = m.entrySet().stream().collect(Collectors.toMap(
                e -> norm(e.getKey()), Map.Entry::getValue, (a,b)->a, LinkedHashMap::new));
    }

    private static String norm(String s){ return s==null? "": s.toLowerCase(Locale.KOREA).replaceAll("\\s+",""); }

    public static List<PlaceCategory> fromKoreanList(List<String> texts){
        if (texts == null) return Collections.emptyList();
        List<PlaceCategory> out = new ArrayList<>();
        for (String t: texts){
            PlaceCategory pc = KMAP.get(norm(t));
            if (pc == null) throw new IllegalArgumentException("선호 장소 인식 불가: " + t);
            out.add(pc);
        }
        return out;
    }
}
