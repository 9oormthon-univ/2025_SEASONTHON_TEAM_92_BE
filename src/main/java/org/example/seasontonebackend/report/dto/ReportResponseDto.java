package org.example.seasontonebackend.report.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReportResponseDto {
    private String primaryNegotiationCard1;
    private String primaryNegotiationCard2;
    private String secondaryNegotiationCard1;
    private String secondaryNegotiationCard2;
    private String step1;
    private String step2;
}
