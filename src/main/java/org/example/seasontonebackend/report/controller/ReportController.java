package org.example.seasontonebackend.report.controller;

import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.report.dto.ReportRequestDto;
import org.example.seasontonebackend.report.dto.ReportResponseDto;
import org.example.seasontonebackend.report.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }


    @PostMapping("/report/create")
    public ResponseEntity<?> createReport(@RequestBody ReportRequestDto reportRequestDto, @AuthenticationPrincipal Member member) {
        Long reportId = reportService.createReport(reportRequestDto, member);
        return new ResponseEntity<>(reportId, HttpStatus.CREATED);
    }


    @GetMapping("/report/{reportId}")
    public ResponseEntity<?> getReport(@PathVariable Long reportId, @AuthenticationPrincipal Member member) {
        try {
            ReportResponseDto reportResponseDto = reportService.getReport(reportId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reportResponseDto);
            response.put("message", "리포트를 조회했습니다.");
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "리포트 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/report/comprehensive")
    public ResponseEntity<?> getComprehensiveReport(@AuthenticationPrincipal Member member) {
        try {
            ReportResponseDto reportResponseDto = reportService.getComprehensiveReport(member);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reportResponseDto);
            response.put("message", "종합 리포트를 조회했습니다.");
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "리포트 생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
