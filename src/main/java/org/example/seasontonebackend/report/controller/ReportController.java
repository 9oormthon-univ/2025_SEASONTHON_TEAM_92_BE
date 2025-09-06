package org.example.seasontonebackend.report.controller;

import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.report.dto.ReportRequestDto;
import org.example.seasontonebackend.report.dto.ReportResponseDto;
import org.example.seasontonebackend.report.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> getReport(@PathVariable Long reportId) {
        ReportResponseDto reportResponseDto = reportService.getReport(reportId);

        return new ResponseEntity<>(reportResponseDto, HttpStatus.OK);
    }
}
