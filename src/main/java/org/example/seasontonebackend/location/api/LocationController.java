package org.example.seasontonebackend.location.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.location.application.LocationService;
import org.example.seasontonebackend.location.dto.LocationVerificationRequest;
import org.example.seasontonebackend.location.dto.LocationVerificationResponse;
import org.example.seasontonebackend.location.dto.AddressPreviewResponse;
import org.example.seasontonebackend.location.dto.GPSVerificationRequest;
import org.example.seasontonebackend.location.dto.GPSVerificationResponse;
import org.example.seasontonebackend.location.dto.LocationAccuracyResponse;
import org.example.seasontonebackend.location.exception.LocationException;
import org.example.seasontonebackend.member.domain.Member;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * GPS 좌표를 이용한 위치 인증
     * userId는 JWT 토큰에서 자동 추출
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyLocation(@RequestBody LocationVerificationRequest request, @AuthenticationPrincipal Member member) {
        log.info("🔥🔥🔥 LocationController.verifyLocation 호출됨!");
        log.info("📥 받은 데이터: {}", request);

        try {
            // @AuthenticationPrincipal을 통해 Member 객체를 직접 받으므로, 별도 추출 로직 불필요
            log.info("🔐 인증된 사용자 ID: {}", member.getId());

            // 기본 유효성 검증
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

            // LocationService에 Member 객체를 직접 전달
            LocationVerificationResponse response = locationService.verifyLocation(request, member);

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

    /**
     * GPS 인증 수행
     */
    @PostMapping("/gps/verify")
    public ResponseEntity<Map<String, Object>> verifyGPSLocation(@RequestBody GPSVerificationRequest request) {
        log.info("🔥🔥🔥 GPS 인증 요청!");
        log.info("📥 받은 데이터: {}", request);

        try {
            // 기본 유효성 검증
            if (request.getLatitude() == null || request.getLongitude() == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("data", null);
                errorResult.put("message", "위도와 경도는 필수입니다.");
                return ResponseEntity.badRequest().body(errorResult);
            }

            if (request.getAccuracy() == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("data", null);
                errorResult.put("message", "정확도 정보는 필수입니다.");
                return ResponseEntity.badRequest().body(errorResult);
            }

            // GPS 인증 수행
            GPSVerificationResponse response = locationService.verifyGPSLocation(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", response.getMessage());

            log.info("✅ GPS 인증 성공! 신뢰도: {}%, 인증: {}", response.getConfidence(), response.isVerified());
            return ResponseEntity.ok(result);

        } catch (LocationException e) {
            log.error("❌ GPS 인증 실패: {}", e.getMessage());

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("data", null);
            errorResult.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResult);

        } catch (Exception e) {
            log.error("❌ GPS 인증 중 예상치 못한 오류 발생", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("data", null);
            errorResult.put("message", "GPS 인증 중 서버 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 위치 정확도 평가
     */
    @PostMapping("/gps/accuracy")
    public ResponseEntity<Map<String, Object>> evaluateLocationAccuracy(@RequestBody GPSVerificationRequest request) {
        log.info("📍 위치 정확도 평가 요청!");
        log.info("📥 받은 데이터: {}", request);

        try {
            // 기본 유효성 검증
            if (request.getLatitude() == null || request.getLongitude() == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("data", null);
                errorResult.put("message", "위도와 경도는 필수입니다.");
                return ResponseEntity.badRequest().body(errorResult);
            }

            // 정확도 평가 수행
            LocationAccuracyResponse response = locationService.evaluateLocationAccuracy(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", response.getMessage());

            log.info("✅ 정확도 평가 성공! 신뢰도: {}%", response.getConfidence());
            return ResponseEntity.ok(result);

        } catch (LocationException e) {
            log.error("❌ 정확도 평가 실패: {}", e.getMessage());

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("data", null);
            errorResult.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResult);

        } catch (Exception e) {
            log.error("❌ 정확도 평가 중 예상치 못한 오류 발생", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("data", null);
            errorResult.put("message", "정확도 평가 중 서버 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
}
