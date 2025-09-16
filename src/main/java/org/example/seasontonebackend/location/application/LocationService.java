package org.example.seasontonebackend.location.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.location.dto.LocationVerificationRequest;
import org.example.seasontonebackend.location.dto.LocationVerificationResponse;
import org.example.seasontonebackend.location.dto.AddressPreviewResponse;
import org.example.seasontonebackend.location.dto.GPSVerificationRequest;
import org.example.seasontonebackend.location.dto.GPSVerificationResponse;
import org.example.seasontonebackend.location.dto.LocationAccuracyResponse;
import org.example.seasontonebackend.location.exception.LocationException;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 위치 인증 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final GeocodingService geocodingService;
    private final MemberRepository memberRepository; // MemberRepository 주입

    /**
     * GPS 좌표를 이용한 위치 인증
     */
    @Transactional // 트랜잭션 추가
    public LocationVerificationResponse verifyLocation(LocationVerificationRequest request, Member member) {
        log.info("🔥 위치 인증 시작 - 사용자 ID: {}", member.getId());
        log.info("📍 좌표 - 경도: {}, 위도: {}", request.getLongitude(), request.getLatitude());
        log.info("🏠 건물명: {}", request.getBuildingName());

        try {
            // 1. GPS 좌표를 주소로 변환
            String address = geocodingService.getAddressFromCoordinates(
                    request.getLongitude(),
                    request.getLatitude()
            );

            if (address == null || address.isEmpty()) {
                throw new LocationException("GPS 좌표에서 주소를 찾을 수 없습니다.");
            }

            // 2. 동네 정보 추출
            String neighborhood = geocodingService.getNeighborhoodFromCoordinates(
                    request.getLongitude(),
                    request.getLatitude()
            );

            // 3. 위치 인증 범위 검증
            if (!geocodingService.isWithinAcceptableRange(
                    request.getLatitude(), request.getLongitude(),
                    request.getLatitude(), request.getLongitude())) {
                throw new LocationException("위치 인증 범위를 벗어났습니다.");
            }

            // 4. 인증 성공 시 Member 엔티티 업데이트
            member.setGpsVerified(true);
            memberRepository.save(member); // 변경사항 저장

            log.info("Member {} saved with gpsVerified = true", member.getId());

            // DEBUGGING STEP:
            Member savedMember = memberRepository.findById(member.getId()).orElseThrow();
            log.info("Re-fetched member {}, gpsVerified status is: {}", savedMember.getId(), savedMember.getGpsVerified());

            // 5. 인증 성공 응답 생성
            LocationVerificationResponse response = LocationVerificationResponse.builder()
                    .userId(String.valueOf(member.getId())) // Member ID 사용
                    .address(address)
                    .neighborhood(neighborhood)
                    .buildingName(request.getBuildingName())
                    .verified(true)
                    .message("위치 인증이 완료되었습니다.")
                    .build();

            log.info("✅ 위치 인증 완료!");
            log.info("📋 인증 결과 - 주소: {}, 동네: {}", address, neighborhood);

            return response;

        } catch (LocationException e) {
            log.error("❌ 위치 인증 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ 위치 인증 중 예상치 못한 오류 발생", e);
            throw new LocationException("위치 인증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * GPS 좌표로부터 주소 미리보기 (인증 없이)
     */
    public AddressPreviewResponse getAddressPreview(double longitude, double latitude) {
        log.info("📍 주소 미리보기 요청 - 경도: {}, 위도: {}", longitude, latitude);

        try {
            String address = geocodingService.getAddressFromCoordinates(longitude, latitude);
            String neighborhood = geocodingService.getNeighborhoodFromCoordinates(longitude, latitude);

            return AddressPreviewResponse.builder()
                    .address(address)
                    .neighborhood(neighborhood)
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();

        } catch (Exception e) {
            log.error("❌ 주소 미리보기 실패 - 좌표: ({}, {})", longitude, latitude, e);
            log.error("예외 타입: {}", e.getClass().getSimpleName());
            log.error("예외 메시지: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("원인 예외: {}", e.getCause().getMessage());
            }

            return AddressPreviewResponse.builder()
                    .address("주소를 가져올 수 없습니다")
                    .neighborhood("동 정보를 가져올 수 없습니다")
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
        }
    }

    /**
     * GPS 인증 수행
     */
    public GPSVerificationResponse verifyGPSLocation(GPSVerificationRequest request) {
        log.info("🔥 GPS 인증 시작");
        log.info("📍 좌표 - 경도: {}, 위도: {}, 정확도: {}m", 
                request.getLongitude(), request.getLatitude(), request.getAccuracy());

        try {
            // 1. GPS 좌표를 주소로 변환
            String address = geocodingService.getAddressFromCoordinates(
                    request.getLongitude(),
                    request.getLatitude()
            );

            if (address == null || address.isEmpty()) {
                throw new LocationException("GPS 좌표에서 주소를 찾을 수 없습니다.");
            }

            // 2. 동네 정보 추출
            String neighborhood = geocodingService.getNeighborhoodFromCoordinates(
                    request.getLongitude(),
                    request.getLatitude()
            );

            // 3. 정확도 기반 신뢰도 계산
            Integer confidence = calculateConfidence(request.getAccuracy());
            
            // 4. 인증 성공 여부 판단 (70% 이상이면 성공)
            boolean isVerified = confidence >= 70;

            // 5. 주소 정보 파싱
            String[] addressParts = address.split(" ");
            String gu = addressParts.length > 1 ? addressParts[1] : "";
            String si = addressParts.length > 0 ? addressParts[0] : "";

            // 6. 응답 생성
            GPSVerificationResponse response = GPSVerificationResponse.builder()
                    .isVerified(isVerified)
                    .confidence(confidence)
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .accuracy(request.getAccuracy())
                    .timestamp(request.getTimestamp())
                    .address(address)
                    .dong(neighborhood)
                    .gu(gu)
                    .si(si)
                    .verificationMethod("gps")
                    .verifiedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .message(isVerified ? "GPS 인증이 완료되었습니다." : "GPS 정확도가 낮아 인증에 실패했습니다.")
                    .build();

            log.info("✅ GPS 인증 완료! 신뢰도: {}%, 인증: {}", confidence, isVerified);
            return response;

        } catch (Exception e) {
            log.error("❌ GPS 인증 중 오류 발생", e);
            throw new LocationException("GPS 인증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 위치 정확도 평가
     */
    public LocationAccuracyResponse evaluateLocationAccuracy(GPSVerificationRequest request) {
        log.info("📍 위치 정확도 평가 - 정확도: {}m", request.getAccuracy());

        try {
            Integer confidence = calculateConfidence(request.getAccuracy());
            List<String> recommendations = generateRecommendations(request.getAccuracy());

            LocationAccuracyResponse response = LocationAccuracyResponse.builder()
                    .accuracy(request.getAccuracy())
                    .confidence(confidence)
                    .recommendations(recommendations)
                    .message("위치 정확도 평가가 완료되었습니다.")
                    .build();

            log.info("✅ 정확도 평가 완료 - 신뢰도: {}%", confidence);
            return response;

        } catch (Exception e) {
            log.error("❌ 정확도 평가 중 오류 발생", e);
            throw new LocationException("정확도 평가 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 정확도 기반 신뢰도 계산
     */
    private Integer calculateConfidence(Double accuracy) {
        if (accuracy == null) return 50;
        
        if (accuracy <= 10) {
            return 95; // 매우 높은 정확도
        } else if (accuracy <= 50) {
            return 85; // 높은 정확도
        } else if (accuracy <= 100) {
            return 70; // 보통 정확도
        } else {
            return 50; // 낮은 정확도
        }
    }

    /**
     * 정확도 개선 추천사항 생성
     */
    private List<String> generateRecommendations(Double accuracy) {
        if (accuracy == null || accuracy <= 50) {
            return Arrays.asList(
                "현재 정확도가 양호합니다",
                "실외에서 측정하세요",
                "GPS 신호가 강한 곳에서 측정하세요"
            );
        } else {
            return Arrays.asList(
                "실외에서 측정하세요",
                "건물이나 나무에서 멀리 떨어지세요",
                "GPS 신호가 강한 곳에서 측정하세요",
                "WiFi와 모바일 데이터를 모두 켜두세요",
                "여러 번 측정하여 평균값을 사용하세요"
            );
        }
    }
}
