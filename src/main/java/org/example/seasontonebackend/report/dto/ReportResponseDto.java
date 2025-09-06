package org.example.seasontonebackend.report.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReportResponseDto {
    private String primaryNegotiationCard;
    private String secondaryNegotiationCard;
//    private String step1;
//    private String step2;
}
