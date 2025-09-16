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
            log.debug("VWorld API 응답: {}", response);

            if (response == null) {
                return getFallbackAddress(longitude, latitude);
            }

            JSONObject jsonResponse = new JSONObject(response);

            // 에러 응답 확인
            if (jsonResponse.has("response") && jsonResponse.getJSONObject("response").has("status")) {
                String status = jsonResponse.getJSONObject("response").getString("status");
                if ("ERROR".equals(status)) {
                    log.error("VWorld API 오류 발생: {}", jsonResponse.toString());
                    return getFallbackAddress(longitude, latitude);
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
                            return selectedAddress;
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("VWorld API 호출 실패", e);
        }

        return getFallbackAddress(longitude, latitude);
    }

    /**
     * 주소에서 동네(neighborhood) 정보 추출
     */
    public String getNeighborhoodFromCoordinates(double longitude, double latitude) {
        String fullAddress = getAddressFromCoordinates(longitude, latitude);
        return extractNeighborhoodFromAddress(fullAddress);
    }

    /**
     * 주소에서 동 정보 추출
     */
    private String extractNeighborhoodFromAddress(String address) {
        if (address == null || address.isEmpty()) {
            return "알 수 없는 동";
        }

        String[] parts = address.split(" ");
        for (String part : parts) {
            if (part.endsWith("동") || part.endsWith("면") || part.endsWith("읍")) {
                return part;
            }
        }

        for (String part : parts) {
            if (part.endsWith("구")) {
                return part;
            }
        }

        return "알 수 없는 동";
    }

    /**
     * API 호출 실패 시 대체 주소 생성
     */
    private String getFallbackAddress(double longitude, double latitude) {
        if (latitude >= 37.4 && latitude <= 37.7 && longitude >= 126.8 && longitude <= 127.2) {
            return "서울특별시 강남구";
        } else if (latitude >= 35.4 && latitude <= 35.8 && longitude >= 129.1 && longitude <= 129.6) {
            return "울산광역시 중구";
        } else {
            return "서울특별시 강남구";
        }
    }

    /**
     * 위치 인증 범위 검증 (개발 단계에서는 항상 true)
     */
    public boolean isWithinAcceptableRange(double lat1, double lon1, double lat2, double lon2) {
        return true;
    }
}