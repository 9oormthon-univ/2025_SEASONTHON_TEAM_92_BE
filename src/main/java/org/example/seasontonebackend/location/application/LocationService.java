package org.example.seasontonebackend.location.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.location.dto.LocationVerificationRequest;
import org.example.seasontonebackend.location.dto.LocationVerificationResponse;
import org.example.seasontonebackend.location.dto.AddressPreviewResponse;
import org.example.seasontonebackend.location.exception.LocationException;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            log.info("Re-fetched member {}, gpsVerified status is: {}", savedMember.getId(), savedMember.isGpsVerified());

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
            log.error("주소 미리보기 실패", e);

            return AddressPreviewResponse.builder()
                    .address("주소를 가져올 수 없습니다")
                    .neighborhood("동 정보를 가져올 수 없습니다")
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
        }
    }
}
