package org.example.seasontonebackend.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResponseDTO {
    private Long diagnosisId;
    private Integer totalScore;
    private Map<String, Object> scores;
    private LocalDateTime diagnosedAt;

    // 비교 데이터
    private Integer buildingRank;
    private Integer dongRank;
    private Integer buildingTotal;
    private Integer dongTotal;
    private Double buildingAverage;
    private Double dongAverage;
}