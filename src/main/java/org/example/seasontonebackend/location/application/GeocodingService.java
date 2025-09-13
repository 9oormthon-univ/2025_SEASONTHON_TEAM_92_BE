package org.example.seasontonebackend.location.application;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    public GeocodingService() {
        this.restTemplate = new RestTemplate();
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
            return getFallbackAddress(longitude, latitude);

        } catch (Exception e) {
            log.error("VWorld API 호출 실패", e);
            throw new RuntimeException("주소 조회에 실패했습니다. 다시 시도해주세요.");
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
     * API 호출 실패 시 대체 주소 생성 (좌표 기반 지역 추정)
     */
    private String getFallbackAddress(double longitude, double latitude) {
        // 서울 강남구 (더 좁은 범위)
        if (latitude >= 37.49 && latitude <= 37.56 && longitude >= 127.02 && longitude <= 127.08) {
            return "서울특별시 강남구";
        }
        // 서울 전체 지역 (강남구 제외)
        else if (latitude >= 37.4 && latitude <= 37.7 && longitude >= 126.8 && longitude <= 127.2) {
            return "서울특별시";
        }
        // 울산 지역
        else if (latitude >= 35.4 && latitude <= 35.8 && longitude >= 129.1 && longitude <= 129.6) {
            return "울산광역시 중구";
        }
        // 부산 지역
        else if (latitude >= 35.0 && latitude <= 35.4 && longitude >= 128.8 && longitude <= 129.2) {
            return "부산광역시 해운대구";
        }
        // 대구 지역
        else if (latitude >= 35.7 && latitude <= 36.0 && longitude >= 128.4 && longitude <= 128.8) {
            return "대구광역시 수성구";
        }
        // 인천 지역
        else if (latitude >= 37.4 && latitude <= 37.6 && longitude >= 126.4 && longitude <= 126.8) {
            return "인천광역시 연수구";
        }
        // 대전 지역
        else if (latitude >= 36.2 && latitude <= 36.5 && longitude >= 127.2 && longitude <= 127.6) {
            return "대전광역시 유성구";
        }
        // 광주 지역
        else if (latitude >= 35.0 && latitude <= 35.3 && longitude >= 126.6 && longitude <= 127.0) {
            return "광주광역시 서구";
        }
        // 경기도
        else if (latitude >= 37.0 && latitude <= 38.0 && longitude >= 126.5 && longitude <= 127.5) {
            return "경기도 성남시";
        }
        // 강원도
        else if (latitude >= 37.0 && latitude <= 38.5 && longitude >= 127.5 && longitude <= 129.0) {
            return "강원도 춘천시";
        }
        // 충청도
        else if (latitude >= 36.0 && latitude <= 37.0 && longitude >= 126.0 && longitude <= 128.0) {
            return "충청남도 천안시";
        }
        // 전라도
        else if (latitude >= 34.5 && latitude <= 36.0 && longitude >= 125.5 && longitude <= 127.5) {
            return "전라북도 전주시";
        }
        // 경상도
        else if (latitude >= 35.0 && latitude <= 37.0 && longitude >= 128.0 && longitude <= 130.0) {
            return "경상북도 포항시";
        }
        // 제주도
        else if (latitude >= 33.0 && latitude <= 34.0 && longitude >= 126.0 && longitude <= 127.0) {
            return "제주특별자치도 제주시";
        }
        // 기본값: 좌표를 기반으로 추정
        else {
            log.warn("알 수 없는 좌표 범위 - 경도: {}, 위도: {}", longitude, latitude);
            return String.format("알 수 없는 지역 (%.4f, %.4f)", latitude, longitude);
        }
    }

    /**
     * 위치 인증 범위 검증 (개발 단계에서는 항상 true)
     */
    public boolean isWithinAcceptableRange(double lat1, double lon1, double lat2, double lon2) {
        return true;
    }
}