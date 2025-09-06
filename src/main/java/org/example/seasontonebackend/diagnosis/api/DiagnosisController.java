package org.example.seasontonebackend.diagnosis.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.diagnosis.application.DiagnosisService;
import org.example.seasontonebackend.diagnosis.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @GetMapping("/questions")
    public ResponseEntity<Map<String, Object>> getQuestions() {
        try {
            DiagnosisQuestionsResponseDTO data = diagnosisService.getQuestions();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "진단 질문을 조회했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("진단 질문 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/responses")
    public ResponseEntity<Map<String, Object>> submitResponses(
            @RequestBody DiagnosisRequestDTO request,
            @AuthenticationPrincipal Member member) {
        try {
            DiagnosisSubmissionResponseDTO data = diagnosisService.submitResponses(member, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "진단 응답이 저장되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("진단 응답 제출 실패 - 사용자: {}, 오류: {}", member.getEmail(), e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/result")
    public ResponseEntity<Map<String, Object>> getResult(@AuthenticationPrincipal Member member) {
        try {
            DiagnosisResultResponseDTO data = diagnosisService.getResult(member);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "진단 결과를 조회했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("진단 결과 조회 실패 - 사용자: {}, 오류: {}", member.getEmail(), e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}