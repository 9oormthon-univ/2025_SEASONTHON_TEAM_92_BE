package org.example.seasontonebackend.report.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequestDto {
    private String reportContent; // 협상 요구사항
    private String reportType; // 'free' 또는 'premium'
}
