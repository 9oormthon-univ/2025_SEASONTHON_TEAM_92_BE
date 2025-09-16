package org.example.seasontonebackend.smartdiagnosis.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class SmartDiagnosisResponseDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelStartResponse {
        private String sessionId;
        private Long measurementId;
        private List<String> instructions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelMeasureResponse {
        private Long measurementId;
        private Double xAxisTilt;
        private Double yAxisTilt;
        private Double totalTilt;
        private Boolean isLevel;
        private String levelStatus;
        private String recommendation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelHistory {
        private Long measurementId;
        private String location;
        private Double totalTilt;
        private String levelStatus;
        private String createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MeasurementDetail {
        private MeasurementInfo measurement;
        private LevelDetail levelDetails;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MeasurementInfo {
        private Long measurementId;
        private String measurementType;
        private String location;
        private Double measuredValue;
        private String unit;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelDetail {
        private Double xAxis;
        private Double yAxis;
        private Double zAxis;
        private Double totalTilt;
        private Boolean isLevel;
        private String levelStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoiseStartResponse {
        private String sessionId;
        private Long measurementId;
        private Integer duration;
        private String startTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoiseRealtimeResponse {
        private Double currentDecibel;
        private String category;
        private String recommendation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoiseCompleteResponse {
        private Long measurementId;
        private Double avgDecibel;
        private Double minDecibel;
        private Double maxDecibel;
        private String category;
        private String comparisonText;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternetStartResponse {
        private String sessionId;
        private Long measurementId;
        private List<TestServer> testServers;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestServer {
        private String name;
        private String location;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternetCompleteResponse {
        private Long measurementId;
        private Double downloadSpeed;
        private Double uploadSpeed;
        private Double ping;
        private String speedGrade;
        private String comparison;
    }

    // 통합 요약 DTO 추가
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SmartDiagnosisSummary {
        private Integer overallScore;
        private MeasurementSummary measurements;
        private List<String> insights;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MeasurementSummary {
        private LevelSummary level;
        private NoiseSummary noise;
        private InternetSummary internet;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelSummary {
        private Double latestValue;
        private String grade;
        private String trend;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoiseSummary {
        private Double latestValue;
        private String grade;
        private String trend;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternetSummary {
        private Double latestValue;
        private String grade;
        private String comparison;
    }
}