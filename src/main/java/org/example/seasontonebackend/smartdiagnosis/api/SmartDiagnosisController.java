package org.example.seasontonebackend.smartdiagnosis.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.smartdiagnosis.application.SmartDiagnosisService;
import org.example.seasontonebackend.smartdiagnosis.dto.SmartDiagnosisRequestDTO;
import org.example.seasontonebackend.smartdiagnosis.dto.SmartDiagnosisResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/smart-diagnosis")
@RequiredArgsConstructor
@Slf4j
public class SmartDiagnosisController {

    private final SmartDiagnosisService smartDiagnosisService;

    // ========== 수평계 API ==========

    @PostMapping("/level/start")
    public ResponseEntity<Map<String, Object>> startLevelMeasurement(
            @RequestBody SmartDiagnosisRequestDTO.LevelStart request,
            @AuthenticationPrincipal Member member) {

        try {
            SmartDiagnosisResponseDTO.LevelStartResponse response =
                    smartDiagnosisService.startLevelMeasurement(member, request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "수평 측정을 시작했습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("수평 측정 시작 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "수평 측정 시작 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    @PostMapping("/level/measure")
    public ResponseEntity<Map<String, Object>> processLevelMeasurement(
            @RequestBody SmartDiagnosisRequestDTO.LevelMeasure request,
            @AuthenticationPrincipal Member member) {

        try {
            SmartDiagnosisResponseDTO.LevelMeasureResponse response =
                    smartDiagnosisService.processLevelMeasurement(member, request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "수평 측정이 완료되었습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("수평 데이터 처리 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "수평 데이터 처리 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    @GetMapping("/level/history")
    public ResponseEntity<Map<String, Object>> getLevelHistory(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal Member member) {

        try {
            List<SmartDiagnosisResponseDTO.LevelHistory> history =
                    smartDiagnosisService.getLevelHistory(member, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", history);
            result.put("message", "수평 측정 기록을 조회했습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("수평 측정 기록 조회 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "기록 조회 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    // ========== 소음 측정 API ==========

    @PostMapping("/noise/start")
    public ResponseEntity<Map<String, Object>> startNoiseMeasurement(
            @RequestBody SmartDiagnosisRequestDTO.NoiseStart request,
            @AuthenticationPrincipal Member member) {

        try {
            SmartDiagnosisResponseDTO.NoiseStartResponse response =
                    smartDiagnosisService.startNoiseMeasurement(member, request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "소음 측정을 시작했습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("소음 측정 시작 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "소음 측정 시작 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    @PostMapping("/noise/realtime")
    public ResponseEntity<Map<String, Object>> processRealtimeNoise(
            @RequestBody SmartDiagnosisRequestDTO.NoiseRealtime request,
            @AuthenticationPrincipal Member member) {

        try {
            SmartDiagnosisResponseDTO.NoiseRealtimeResponse response =
                    smartDiagnosisService.processRealtimeNoise(member, request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "실시간 소음 데이터를 처리했습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("실시간 소음 데이터 처리 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "실시간 데이터 처리 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    @PostMapping("/noise/complete")
    public ResponseEntity<Map<String, Object>> completeNoiseMeasurement(
            @RequestBody SmartDiagnosisRequestDTO.NoiseComplete request,
            @AuthenticationPrincipal Member member) {

        try {
            SmartDiagnosisResponseDTO.NoiseCompleteResponse response =
                    smartDiagnosisService.completeNoiseMeasurement(member, request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "소음 측정이 완료되었습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("소음 측정 완료 처리 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "소음 측정 완료 처리 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    // ========== 인터넷 속도 측정 API ==========

    @PostMapping("/internet/start")
    public ResponseEntity<Map<String, Object>> startInternetSpeedTest(
            @RequestBody SmartDiagnosisRequestDTO.InternetStart request,
            @AuthenticationPrincipal Member member) {

        try {
            SmartDiagnosisResponseDTO.InternetStartResponse response =
                    smartDiagnosisService.startInternetSpeedTest(member, request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "인터넷 속도 측정을 시작했습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("인터넷 속도 측정 시작 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "인터넷 속도 측정 시작 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    @PostMapping("/internet/complete")
    public ResponseEntity<Map<String, Object>> completeInternetSpeedTest(
            @RequestBody SmartDiagnosisRequestDTO.InternetComplete request,
            @AuthenticationPrincipal Member member) {

        try {
            SmartDiagnosisResponseDTO.InternetCompleteResponse response =
                    smartDiagnosisService.completeInternetSpeedTest(member, request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "인터넷 속도 측정이 완료되었습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("인터넷 속도 측정 완료 처리 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "인터넷 속도 측정 완료 처리 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    // ========== 통합 조회 API ==========

    @GetMapping("/measurements/{measurementId}")
    public ResponseEntity<Map<String, Object>> getMeasurementDetail(
            @PathVariable Long measurementId,
            @AuthenticationPrincipal Member member) {

        try {
            SmartDiagnosisResponseDTO.MeasurementDetail detail =
                    smartDiagnosisService.getMeasurementDetail(member, measurementId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", detail);
            result.put("message", "측정 상세 정보를 조회했습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("측정 상세 조회 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "측정 상세 조회 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSmartDiagnosisSummary(
            @AuthenticationPrincipal Member member) {

        try {
            SmartDiagnosisResponseDTO.SmartDiagnosisSummary summary =
                    smartDiagnosisService.getSmartDiagnosisSummary(member);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", summary);
            result.put("message", "스마트 진단 종합 결과를 조회했습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("스마트 진단 종합 결과 조회 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "종합 결과 조회 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
}