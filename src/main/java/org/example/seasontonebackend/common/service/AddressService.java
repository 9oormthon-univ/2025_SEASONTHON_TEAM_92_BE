package org.example.seasontonebackend.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AddressService {
    
    // 서울시 구별 법정동코드 매핑
    private static final Map<String, String> GU_CODE_MAP = new HashMap<>();
    
    // 서울시 동별 법정동코드 매핑 (주요 동들)
    private static final Map<String, String> DONG_CODE_MAP = new HashMap<>();
    
    static {
        // 구별 법정동코드
        GU_CODE_MAP.put("강남구", "11680");
        GU_CODE_MAP.put("강동구", "11740");
        GU_CODE_MAP.put("강북구", "11305");
        GU_CODE_MAP.put("강서구", "11500");
        GU_CODE_MAP.put("관악구", "11620");
        GU_CODE_MAP.put("광진구", "11215");
        GU_CODE_MAP.put("구로구", "11530");
        GU_CODE_MAP.put("금천구", "11545");
        GU_CODE_MAP.put("노원구", "11350");
        GU_CODE_MAP.put("도봉구", "11320");
        GU_CODE_MAP.put("동대문구", "11230");
        GU_CODE_MAP.put("동작구", "11590");
        GU_CODE_MAP.put("마포구", "11440");
        GU_CODE_MAP.put("서대문구", "11410");
        GU_CODE_MAP.put("서초구", "11650");
        GU_CODE_MAP.put("성동구", "11200");
        GU_CODE_MAP.put("성북구", "11290");
        GU_CODE_MAP.put("송파구", "11710");
        GU_CODE_MAP.put("양천구", "11470");
        GU_CODE_MAP.put("영등포구", "11560");
        GU_CODE_MAP.put("용산구", "11170");
        GU_CODE_MAP.put("은평구", "11380");
        GU_CODE_MAP.put("종로구", "11110");
        GU_CODE_MAP.put("중구", "11140");
        GU_CODE_MAP.put("중랑구", "11260");
        
        // 울산광역시 구별 법정동코드
        GU_CODE_MAP.put("울산중구", "31110"); // 울산 중구
        GU_CODE_MAP.put("울산남구", "31140"); // 울산 남구
        GU_CODE_MAP.put("울산동구", "31170"); // 울산 동구
        GU_CODE_MAP.put("울산북구", "31200"); // 울산 북구
        GU_CODE_MAP.put("울주군", "31710"); // 울산 울주군
        
        // 주요 동별 법정동코드 (서대문구 예시)
        DONG_CODE_MAP.put("미근동", "1141010100");
        DONG_CODE_MAP.put("창천동", "1141010200");
        DONG_CODE_MAP.put("충정로2가", "1141010300");
        DONG_CODE_MAP.put("홍제동", "1141010400");
        DONG_CODE_MAP.put("남가좌동", "1141010500");
        DONG_CODE_MAP.put("합동", "1141010600");
        
        // 강남구 주요 동들
        DONG_CODE_MAP.put("역삼동", "1168010100");
        DONG_CODE_MAP.put("개포동", "1168010200");
        DONG_CODE_MAP.put("청담동", "1168010300");
        DONG_CODE_MAP.put("삼성동", "1168010400");
        DONG_CODE_MAP.put("대치동", "1168010500");
        DONG_CODE_MAP.put("논현동", "1168010600");
        
        // 서초구 주요 동들
        DONG_CODE_MAP.put("서초동", "1165010100");
        DONG_CODE_MAP.put("방배동", "1165010200");
        DONG_CODE_MAP.put("잠원동", "1165010300");
        DONG_CODE_MAP.put("반포동", "1165010400");
        DONG_CODE_MAP.put("내곡동", "1165010500");
        DONG_CODE_MAP.put("양재동", "1165010600");
        
        // 마포구 주요 동들
        DONG_CODE_MAP.put("공덕동", "1144010100");
        DONG_CODE_MAP.put("아현동", "1144010200");
        DONG_CODE_MAP.put("도화동", "1144010300");
        DONG_CODE_MAP.put("용강동", "1144010400");
        DONG_CODE_MAP.put("대흥동", "1144010500");
        DONG_CODE_MAP.put("염리동", "1144010600");
        
        // 용산구 주요 동들
        DONG_CODE_MAP.put("후암동", "1117010100");
        DONG_CODE_MAP.put("용산동", "1117010200");
        DONG_CODE_MAP.put("남영동", "1117010300");
        DONG_CODE_MAP.put("청파동", "1117010400");
        DONG_CODE_MAP.put("원효로동", "1117010500");
        DONG_CODE_MAP.put("이촌동", "1117010600");
        
        // 울산 동구 주요 동들
        DONG_CODE_MAP.put("일산동", "3117010100");
        DONG_CODE_MAP.put("방어동", "3117010200");
        DONG_CODE_MAP.put("화정동", "3117010300");
        DONG_CODE_MAP.put("동부동", "3117010400");
        DONG_CODE_MAP.put("서부동", "3117010500");
        DONG_CODE_MAP.put("전하동", "3117010600");
    }
    
    /**
     * 주소 문자열에서 법정동코드 추출
     * @param address 주소 문자열 (예: "서울시 서대문구 미근동", "미근동", "서대문구 미근동")
     * @return 법정동코드 (5자리 구코드 또는 10자리 동코드)
     */
    public String extractLawdCd(String address) {
        if (address == null || address.trim().isEmpty()) {
            log.warn("주소가 비어있음");
            return "11410"; // 기본값: 서대문구
        }
        
        String cleanAddress = address.trim();
        log.info("주소에서 법정동코드 추출 시도: {}", cleanAddress);
        
        // 1. 정확한 동명 매칭 시도
        for (Map.Entry<String, String> entry : DONG_CODE_MAP.entrySet()) {
            if (cleanAddress.contains(entry.getKey())) {
                String lawdCd = entry.getValue();
                log.info("동명 매칭 성공: {} -> {}", entry.getKey(), lawdCd);
                return lawdCd;
            }
        }
        
        // 2. 구명 매칭 시도
        for (Map.Entry<String, String> entry : GU_CODE_MAP.entrySet()) {
            if (cleanAddress.contains(entry.getKey())) {
                String lawdCd = entry.getValue();
                log.info("구명 매칭 성공: {} -> {}", entry.getKey(), lawdCd);
                return lawdCd;
            }
        }
        
        // 3. 매칭 실패 시 기본값 반환
        log.warn("주소 매칭 실패, 기본값 반환: {}", cleanAddress);
        return "11410"; // 기본값: 서대문구
    }
    
    /**
     * 법정동코드에서 구명 추출
     * @param lawdCd 법정동코드
     * @return 구명
     */
    public String getGuNameFromLawdCd(String lawdCd) {
        if (lawdCd == null || lawdCd.length() < 5) {
            return "서대문구";
        }
        
        String guCode = lawdCd.substring(0, 5);
        
        for (Map.Entry<String, String> entry : GU_CODE_MAP.entrySet()) {
            if (entry.getValue().equals(guCode)) {
                return entry.getKey();
            }
        }
        
        return "서대문구"; // 기본값
    }
    
    /**
     * 법정동코드에서 동명 추출
     * @param lawdCd 법정동코드
     * @return 동명
     */
    public String getDongNameFromLawdCd(String lawdCd) {
        if (lawdCd == null || lawdCd.length() < 10) {
            return "미근동";
        }
        
        for (Map.Entry<String, String> entry : DONG_CODE_MAP.entrySet()) {
            if (entry.getValue().equals(lawdCd)) {
                return entry.getKey();
            }
        }
        
        return "미근동"; // 기본값
    }
    
    /**
     * 주소 정규화 (표준 형식으로 변환)
     * @param address 원본 주소
     * @return 정규화된 주소
     */
    public String normalizeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "서울시 서대문구 미근동";
        }
        
        String cleanAddress = address.trim();
        
        // "서울시" 또는 "서울" 제거
        cleanAddress = cleanAddress.replaceAll("서울시\\s*", "").replaceAll("서울\\s*", "");
        
        // 구명이 없으면 서대문구 추가
        boolean hasGu = GU_CODE_MAP.keySet().stream().anyMatch(cleanAddress::contains);
        if (!hasGu) {
            cleanAddress = "서대문구 " + cleanAddress;
        }
        
        return cleanAddress;
    }
}
