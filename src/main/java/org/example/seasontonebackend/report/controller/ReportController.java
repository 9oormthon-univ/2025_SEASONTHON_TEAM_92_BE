package org.example.seasontonebackend.report.controller;

import lombok.Data;
import org.example.seasontonebackend.common.service.EmailService;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.report.dto.ReportRequestDto;
import org.example.seasontonebackend.report.dto.ReportResponseDto;
import org.example.seasontonebackend.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class ReportController {
    private final ReportService reportService;
    private final EmailService emailService; // EmailService 주입

    public ReportController(ReportService reportService, @Autowired(required = false) EmailService emailService) {
        this.reportService = reportService;
        this.emailService = emailService; // 생성자에서 초기화
    }

    // 이메일 요청을 위한 DTO
    @Data
    static class EmailRequest {
        private String to;
        private String content;
    }

    @PostMapping("/api/report/send-email")
    public ResponseEntity<?> sendDocumentByEmail(@RequestBody EmailRequest emailRequest) {
        try {
            if (emailService == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "메일 서비스가 설정되지 않았습니다.");
                return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
            }
            
            String subject = "월세의 정석: 생성된 법적 문서입니다.";
            emailService.sendSimpleMessage(emailRequest.getTo(), subject, emailRequest.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "이메일이 성공적으로 발송되었습니다.");

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "이메일 발송 중 오류가 발생했습니다: " + e.getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/report/create")
    public ResponseEntity<?> createReport(@RequestBody ReportRequestDto reportRequestDto, @AuthenticationPrincipal Member member) {
        try {
            String publicId = reportService.createReport(reportRequestDto, member);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("publicId", publicId);
            response.put("shareUrl", "/report/" + publicId);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "리포트 생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // 비동기 리포트 생성 API (대용량 동시 처리용)
    @PostMapping("/report/create-async")
    public ResponseEntity<?> createReportAsync(@RequestBody ReportRequestDto reportRequestDto, @AuthenticationPrincipal Member member) {
        try {
            CompletableFuture<String> future = reportService.createReportAsync(reportRequestDto, member);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "리포트 생성이 시작되었습니다. 잠시 후 완료됩니다.");
            response.put("processing", true);
            
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "리포트 생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

    // 공개 리포트 조회 API (비회원도 접근 가능)
    @GetMapping("/public/report/{publicId}")
    public ResponseEntity<?> getPublicReport(@PathVariable String publicId) {
        try {
            ReportResponseDto reportResponseDto = reportService.getReportByPublicId(publicId);

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

    // 공유 URL 생성 API
    @PostMapping("/report/share-url")
    public ResponseEntity<?> generateShareUrl(@RequestBody Map<String, Object> request, @AuthenticationPrincipal Member member) {
        try {
            String reportId = (String) request.get("reportId");
            Boolean isPremium = (Boolean) request.getOrDefault("isPremium", false);
            
            String shareUrl = reportService.generateShareUrl(reportId, isPremium);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of("shareUrl", shareUrl));
            response.put("message", "공유 URL이 생성되었습니다.");
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "공유 URL 생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
