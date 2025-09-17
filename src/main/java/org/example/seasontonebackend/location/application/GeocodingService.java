package org.example.seasontonebackend.location.application;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * VWorld API를 사용한 지오코딩 서비스 (SeasonToneBackend용)
 */
@Slf4j
@Service
public class GeocodingService {

    private final RestTemplate restTemplate;

    @Value("${vworld.api.key}")
    private String apiKey;

    @Value("${vworld.api.url}")
    private String apiUrl;
    
    @Value("${vworld.api.enabled:true}")
    private boolean apiEnabled;

    // 주소 -> 법정동 코드 변환을 위한 캐시/폴백 맵
    private static final Map<String, String> LAWD_CODE_MAP = Map.ofEntries(
        Map.entry("강남구", "11680"), Map.entry("강동구", "11740"), Map.entry("강북구", "11305"),
        Map.entry("강서구", "11500"), Map.entry("관악구", "11620"), Map.entry("광진구", "11215"),
        Map.entry("구로구", "11530"), Map.entry("금천구", "11545"), Map.entry("노원구", "11350"),
        Map.entry("도봉구", "11320"), Map.entry("동대문구", "11230"), Map.entry("동작구", "11590"),
        Map.entry("마포구", "11410"), Map.entry("서대문구", "11440"), Map.entry("서초구", "11650"),
        Map.entry("성동구", "11200"), Map.entry("성북구", "11290"), Map.entry("송파구", "11710"),
        Map.entry("양천구", "11470"), Map.entry("영등포구", "11560"), Map.entry("용산구", "11170"),
        Map.entry("은평구", "11380"), Map.entry("종로구", "11110"), Map.entry("중구", "11140"),
        Map.entry("중랑구", "11260")
    );

    public GeocodingService() {
        this.restTemplate = new RestTemplate();
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("--- GeocodingService Initialization ---");
        log.info("VWorld API URL loaded: {}", apiUrl);
        log.info("VWorld API Key loaded: {}", apiKey != null && !apiKey.isEmpty() ? "********" : "null");
        log.info("------------------------------------");
    }

    /**
     * 주소 문자열로부터 법정동 코드를 조회. VWorld API를 우선 사용하고, 실패 시 내부 맵에서 찾습니다.
     */
    public String getLawdCodeFromAddress(String address) {
        log.info("주소로부터 법정동 코드 조회 시작: {}", address);
        if (!apiEnabled) {
            log.warn("VWorld API is disabled. Falling back to local map.");
            return findLawdCodeFromMap(address);
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("service", "search")
                .queryParam("request", "search")
                .queryParam("version", "2.0")
                .queryParam("crs", "epsg:4326")
                .queryParam("size", "1")
                .queryParam("page", "1")
                .queryParam("query", address)
                .queryParam("type", "address")
                .queryParam("category", "road")
                .queryParam("format", "json")
                .queryParam("errorFormat", "json")
                .queryParam("key", apiKey)
                .toUriString();

            log.info("VWorld 주소 검색 API 요청 URL: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            log.info("VWorld 주소 검색 API 응답: {}", response);

            JSONObject jsonResponse = new JSONObject(response);
            String status = jsonResponse.getJSONObject("response").getString("status");

            if ("OK".equals(status)) {
                JSONArray items = jsonResponse.getJSONObject("response").getJSONObject("result").getJSONObject("items").getJSONArray("item");
                if (items.length() > 0) {
                    String lawdCd = items.getJSONObject(0).getJSONObject("address").getString("bcode");
                    if (lawdCd != null && !lawdCd.isEmpty()) {
                        log.info("VWorld API에서 법정동 코드 조회 성공: {}", lawdCd);
                        return lawdCd.substring(0, 5); // 10자리 코드 중 앞 5자리(구 코드)만 사용
                    }
                }
            }
            log.warn("VWorld API에서 주소를 찾지 못했습니다. 로컬 맵에서 다시 시도합니다.");
            return findLawdCodeFromMap(address);
        } catch (Exception e) {
            log.error("VWorld 주소 검색 API 호출 실패. 로컬 맵에서 다시 시도합니다. 에러: {}", e.getMessage());
            return findLawdCodeFromMap(address);
        }
    }

    private String findLawdCodeFromMap(String address) {
        return LAWD_CODE_MAP.entrySet().stream()
            .filter(entry -> address.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse("11410"); // 기본값: 마포구
    }

    /**
     * GPS 좌표를 주소로 변환
     */
    public String getAddressFromCoordinates(double longitude, double latitude) {
        log.info("=== VWorld API 호출 시작 ===");
        log.info("입력 좌표 - 경도: {}, 위도: {}", longitude, latitude);

        try {
            String url = String.format(
                    "%s?service=address&request=GetAddress&version=2.0&crs=epsg:4326&point=%f,%f&format=json&type=both&zipcode=false&simple=false&key=%s",
                    apiUrl, longitude, latitude, apiKey
            );
            
            log.info("🌐 VWorld API 요청 URL: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            log.info("VWorld API 응답: {}", response);

            if (response == null) {
                log.error("VWorld API 응답이 null입니다.");
                throw new RuntimeException("주소 조회에 실패했습니다. 다시 시도해주세요.");
            }

            JSONObject jsonResponse = new JSONObject(response);

            // 에러 응답 확인
            if (jsonResponse.has("response") && jsonResponse.getJSONObject("response").has("status")) {
                String status = jsonResponse.getJSONObject("response").getString("status");
                if ("ERROR".equals(status)) {
                    log.error("VWorld API 오류 발생: {}", jsonResponse.toString());
                    throw new RuntimeException("주소 조회에 실패했습니다. 다시 시도해주세요.");
                }
            }

            // 정상 응답 처리
            if (jsonResponse.has("response") && jsonResponse.getJSONObject("response").has("result")) {
                JSONObject responseObj = jsonResponse.getJSONObject("response");

                if (responseObj.get("result") instanceof org.json.JSONArray) {
                    org.json.JSONArray resultArray = responseObj.getJSONArray("result");

                    if (resultArray.length() > 0) {
                        String roadAddress = null;
                        String parcelAddress = null;

                        for (int i = 0; i < resultArray.length(); i++) {
                            JSONObject item = resultArray.getJSONObject(i);

                            if (item.has("text") && item.has("type")) {
                                String address = item.getString("text");
                                String type = item.getString("type");

                                if ("road".equals(type)) {
                                    roadAddress = address;
                                } else if ("parcel".equals(type)) {
                                    parcelAddress = address;
                                }
                            }
                        }

                        String selectedAddress = roadAddress != null ? roadAddress : parcelAddress;
                        if (selectedAddress != null) {
                            log.info("✅ 주소 변환 성공: {}", selectedAddress);
                            log.info("도로명주소: {}", roadAddress);
                            log.info("지번주소: {}", parcelAddress);
                            return selectedAddress;
                        }
                    }
                }
            }
            
            // API 응답은 정상이지만 주소를 찾지 못한 경우
            log.warn("API 응답은 정상이지만 주소를 찾을 수 없습니다. 좌표: ({}, {})", longitude, latitude);
            return null;

        } catch (Exception e) {
            log.error("❌ VWorld API 호출 실패 - 좌표: ({}, {})", longitude, latitude, e);
            log.error("예외 타입: {}", e.getClass().getSimpleName());
            log.error("예외 메시지: {}", e.getMessage());
            
            // API 호출 실패 시 null 반환 (LocationService에서 처리)
            return null;
        }
    }

    /**
     * 주소에서 동네(neighborhood) 정보 추출
     */
    public String getNeighborhoodFromCoordinates(double longitude, double latitude) {
        String fullAddress = getAddressFromCoordinates(longitude, latitude);
        return extractNeighborhoodFromAddress(fullAddress);
    }

    /**
     * 주소에서 동 정보 추출 (시/도, 구/군, 동/읍/면 포함)
     */
    private String extractNeighborhoodFromAddress(String address) {
        if (address == null || address.isEmpty()) {
            return "알 수 없는 동";
        }

        log.info("주소 파싱 시작: {}", address);
        String[] parts = address.split(" ");
        StringBuilder result = new StringBuilder();
        
        // 시/도 찾기
        for (String part : parts) {
            if (part.endsWith("시") || part.endsWith("도") || part.endsWith("특별시") || part.endsWith("광역시")) {
                result.append(part).append(" ");
                log.info("시/도 발견: {}", part);
                break;
            }
        }
        
        // 구/군 찾기
        for (String part : parts) {
            if (part.endsWith("구") || part.endsWith("군")) {
                result.append(part).append(" ");
                log.info("구/군 발견: {}", part);
                break;
            }
        }
        
        // 동/읍/면 찾기
        for (String part : parts) {
            if (part.endsWith("동") || part.endsWith("면") || part.endsWith("읍")) {
                result.append(part);
                log.info("동/읍/면 발견: {}", part);
                break;
            }
        }
        
        String resultStr = result.toString().trim();
        log.info("최종 파싱 결과: {}", resultStr);
        return resultStr.isEmpty() ? "알 수 없는 동" : resultStr;
    }

    /**
     * 주소를 표준화된 형태로 파싱하여 AddressComponents 반환
     */
    public AddressComponents parseAddressComponents(String address) {
        if (address == null || address.isEmpty()) {
            return new AddressComponents("알 수 없음", "알 수 없음", "알 수 없음", "알 수 없음");
        }

        log.info("표준화된 주소 파싱 시작: {}", address);
        String[] parts = address.split(" ");
        
        String si = "";
        String gu = "";
        String dong = "";
        String fullAddress = address;
        
        // 시/도 찾기
        for (String part : parts) {
            if (part.endsWith("시") || part.endsWith("도") || part.endsWith("특별시") || part.endsWith("광역시")) {
                si = part;
                log.info("시/도 발견: {}", si);
                break;
            }
        }
        
        // 구/군 찾기
        for (String part : parts) {
            if (part.endsWith("구") || part.endsWith("군")) {
                gu = part;
                log.info("구/군 발견: {}", gu);
                break;
            }
        }
        
        // 동/읍/면 찾기
        for (String part : parts) {
            if (part.endsWith("동") || part.endsWith("면") || part.endsWith("읍")) {
                dong = part;
                log.info("동/읍/면 발견: {}", dong);
                break;
            }
        }
        
        AddressComponents components = new AddressComponents(si, gu, dong, fullAddress);
        log.info("표준화된 주소 파싱 완료: {}", components);
        return components;
    }

    /**
     * 주소 구성 요소를 담는 내부 클래스
     */
    public static class AddressComponents {
        private final String si;      // 시/도
        private final String gu;      // 구/군  
        private final String dong;    // 동/읍/면
        private final String fullAddress; // 전체 주소

        public AddressComponents(String si, String gu, String dong, String fullAddress) {
            this.si = si.isEmpty() ? "알 수 없음" : si;
            this.gu = gu.isEmpty() ? "알 수 없음" : gu;
            this.dong = dong.isEmpty() ? "알 수 없음" : dong;
            this.fullAddress = fullAddress;
        }

        public String getSi() { return si; }
        public String getGu() { return gu; }
        public String getDong() { return dong; }
        public String getFullAddress() { return fullAddress; }
        
        public String getFormattedAddress() {
            return String.format("%s %s %s", si, gu, dong).trim();
        }

        @Override
        public String toString() {
            return String.format("AddressComponents{si='%s', gu='%s', dong='%s', full='%s'}", 
                    si, gu, dong, fullAddress);
        }
    }


    /**
     * 위치 인증 범위 검증 (개발 단계에서는 항상 true)
     */
    public boolean isWithinAcceptableRange(double lat1, double lon1, double lat2, double lon2) {
        return true;
    }
}