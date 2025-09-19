package org.example.seasontonebackend.location.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.location.application.LocationService;
import org.example.seasontonebackend.location.dto.LocationVerificationRequest;
import org.example.seasontonebackend.location.dto.LocationVerificationResponse;
import org.example.seasontonebackend.location.dto.AddressPreviewResponse;
import org.example.seasontonebackend.location.exception.LocationException;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 위치 인증 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://2025-seasonthon-team-92-fe.vercel.app",
        "https://*.vercel.app"
})
public class LocationController {

    private final LocationService locationService;
    private final MemberRepository memberRepository;

    /**
     * GPS 좌표를 이용한 위치 인증
     * userId는 JWT 토큰에서 자동 추출
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyLocation(@RequestBody LocationVerificationRequest request) {
        log.info("🔥🔥🔥 LocationController.verifyLocation 호출됨!");
        log.info("📥 받은 데이터: {}", request);

        try {
            // JWT 토큰에서 사용자 ID 추출
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication.getName(); // 또는 토큰에서 추출하는 다른 방법

            log.info("🔐 토큰에서 추출된 사용자 ID: {}", userId);

            // 기본 유효성 검증
            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("data", null);
                errorResult.put("message", "인증되지 않은 사용자입니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResult);
            }

            if (request.getLatitude() == null || request.getLongitude() == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("data", null);
                errorResult.put("message", "위도와 경도는 필수입니다.");
                return ResponseEntity.badRequest().body(errorResult);
            }

            if (request.getBuildingName() == null || request.getBuildingName().trim().isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("data", null);
                errorResult.put("message", "건물명은 필수입니다.");
                return ResponseEntity.badRequest().body(errorResult);
            }

            // userId로 Member 객체 조회
            Optional<Member> memberOpt = memberRepository.findById(Long.parseLong(userId));
            if (memberOpt.isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("data", null);
                errorResult.put("message", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResult);
            }
            Member member = memberOpt.get();

            // userId를 포함한 완전한 요청 객체 생성
            LocationVerificationRequest fullRequest = LocationVerificationRequest.builder()
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .buildingName(request.getBuildingName())
                    .build();

            LocationVerificationResponse response = locationService.verifyLocation(fullRequest, member);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "위치 인증이 완료되었습니다.");

            log.info("✅ 위치 인증 성공!");
            return ResponseEntity.ok(result);

        } catch (LocationException e) {
            log.error("❌ 위치 인증 실패: {}", e.getMessage());

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("data", null);
            errorResult.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResult);

        } catch (Exception e) {
            log.error("❌ 위치 인증 중 예상치 못한 오류 발생", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("data", null);
            errorResult.put("message", "위치 인증 중 서버 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * GPS 좌표로부터 주소 미리보기 (인증 없이)
     */
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> getAddressPreview(
            @RequestParam double longitude,
            @RequestParam double latitude) {

        log.info("📍 주소 미리보기 요청 - 경도: {}, 위도: {}", longitude, latitude);

        try {
            AddressPreviewResponse response = locationService.getAddressPreview(longitude, latitude);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "주소 조회 성공");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("주소 미리보기 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("data", null);
            errorResult.put("message", "주소 조회 중 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 주소 문자열로부터 법정동 코드를 조회 (인증 없이)
     */
    @GetMapping("/lawd-code")
    public ResponseEntity<Map<String, Object>> getLawdCodeFromAddress(@RequestParam String address) {
        log.info("🔎 법정동 코드 조회 요청 - 주소: {}", address);
        try {
            Map<String, String> lawdCodeMap = locationService.getLawdCode(address);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", lawdCodeMap);
            result.put("message", "법정동 코드 조회 성공");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ 법정동 코드 조회 실패", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("data", null);
            errorResult.put("message", "법정동 코드 조회 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * API 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "location");
        status.put("status", "healthy");
        status.put("timestamp", System.currentTimeMillis());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", status);
        result.put("message", "Location service is healthy");

        return ResponseEntity.ok(result);
    }
}