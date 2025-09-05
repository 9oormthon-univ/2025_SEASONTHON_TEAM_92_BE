package org.example.seasontonebackend.diagnosis.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.diagnosis.application.DiagnosisService;
import org.example.seasontonebackend.diagnosis.dto.DiagnosisRequestDTO;

import java.util.Map;

@RestController
@RequestMapping("/api/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping
    public ResponseEntity<?> createOrUpdateDiagnosis(@RequestBody DiagnosisRequestDTO request,
                                                     @AuthenticationPrincipal Member member) {
        try {
            var diagnosis = diagnosisService.createOrUpdateDiagnosis(member.getId(), request);
            return ResponseEntity.ok(Map.of("success", true, "data", diagnosis));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyDiagnosis(@AuthenticationPrincipal Member member) {
        try {
            var diagnosis = diagnosisService.getMyDiagnosis(member.getId());
            return ResponseEntity.ok(Map.of("success", true, "data", diagnosis));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/has-diagnosis")
    public ResponseEntity<?> hasInitialDiagnosis(@AuthenticationPrincipal Member member) {
        try {
            var hasDiagnosis = diagnosisService.hasInitialDiagnosis(member.getId());
            return ResponseEntity.ok(Map.of("success", true, "data", hasDiagnosis));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}