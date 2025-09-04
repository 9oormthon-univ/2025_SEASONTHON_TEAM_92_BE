package org.example.seasontonebackend.officetel.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.officetel.dto.OfficetelMarketDataDto;
import org.example.seasontonebackend.officetel.dto.OfficetelTransactionDto;
import org.example.seasontonebackend.officetel.service.OfficetelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/officetel")
@RequiredArgsConstructor
@Validated
public class OfficetelController {

    private final OfficetelService officetelService;

    /**
     * 건물별 오피스텔 거래 내역 조회
     */
    @GetMapping(value = "/rent-data", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Map<String, Object>> getRentData(
            @RequestParam("lawdCd")
            @Pattern(regexp = "^[0-9]{5}$", message = "법정동코드는 5자리 숫자여야 합니다")
            String lawdCd,
            @AuthenticationPrincipal Member member) {

        log.info("오피스텔 거래내역 조회 요청 - 사용자: {}, 법정동코드: {}", member.getEmail(), lawdCd);

        try {
            Map<String, List<OfficetelTransactionDto>> data = officetelService.getOfficetelRentData(lawdCd);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "거래 내역을 성공적으로 조회했습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("거래내역 조회 실패 - 사용자: {}, 법정동코드: {}, 오류: {}", member.getEmail(), lawdCd, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "거래 내역 조회 중 오류가 발생했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 전세 시세 조회
     */
    @GetMapping(value = "/jeonse-market", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Map<String, Object>> getJeonseMarket(
            @RequestParam("lawdCd")
            @Pattern(regexp = "^[0-9]{5}$", message = "법정동코드는 5자리 숫자여야 합니다")
            String lawdCd,
            @AuthenticationPrincipal Member member) {

        log.info("전세 시세 조회 요청 - 사용자: {}, 법정동코드: {}", member.getEmail(), lawdCd);

        try {
            List<OfficetelMarketDataDto> data = officetelService.getJeonseMarketData(lawdCd);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "전세 시세를 성공적으로 조회했습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("전세 시세 조회 실패 - 사용자: {}, 법정동코드: {}, 오류: {}", member.getEmail(), lawdCd, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "전세 시세 조회 중 오류가 발생했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 월세 시세 조회
     */
    @GetMapping(value = "/monthly-rent-market", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Map<String, Object>> getMonthlyRentMarket(
            @RequestParam("lawdCd")
            @Pattern(regexp = "^[0-9]{5}$", message = "법정동코드는 5자리 숫자여야 합니다")
            String lawdCd,
            @AuthenticationPrincipal Member member) {

        log.info("월세 시세 조회 요청 - 사용자: {}, 법정동코드: {}", member.getEmail(), lawdCd);

        try {
            List<OfficetelMarketDataDto> data = officetelService.getMonthlyRentMarketData(lawdCd);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "월세 시세를 성공적으로 조회했습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("월세 시세 조회 실패 - 사용자: {}, 법정동코드: {}, 오류: {}", member.getEmail(), lawdCd, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "월세 시세 조회 중 오류가 발생했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}