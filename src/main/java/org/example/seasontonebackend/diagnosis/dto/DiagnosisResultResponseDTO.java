package org.example.seasontonebackend.diagnosis.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisResultResponseDTO {
    private Summary summary;
    private List<CategoryDetail> categoryDetails;
    private Analysis analysis;
    private Statistics statistics;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private Integer totalScore;
        private String grade;
        private Double buildingAverage;
        private Double neighborhoodAverage;
        private Integer buildingRank;
        private Integer neighborhoodRank;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryDetail {
        private Long categoryId;
        private Double myScore;
        private Double buildingAverage;
        private Double neighborhoodAverage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Analysis {
        private List<AnalysisItem> strengths;
        private List<AnalysisItem> improvements;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisItem {
        private Long categoryId;
        private Integer score;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Statistics {
        private Integer participantCount;
        private Integer responseCount;
        private Integer buildingResidents;
        private Integer neighborhoodResidents;
    }
}