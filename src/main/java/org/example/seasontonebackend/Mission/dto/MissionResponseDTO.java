package org.example.seasontonebackend.Mission.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class MissionResponseDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentMission {
        private Long missionId;
        private String category;
        private String title;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<MissionQuestion> questions;
        private Integer participationCount;
        private Boolean userParticipated;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MissionQuestion {
        private Long questionId;
        private String questionText;
        private String questionType;
        private List<String> options;
        private Integer orderNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MissionResult {
        private Integer userScore;
        private Integer maxScore;
        private String category;
        private ComparisonData buildingComparison;
        private ComparisonData neighborhoodComparison;
        private List<String> insights;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComparisonData {
        private Double average;
        private Integer userRank;
        private Integer totalParticipants;
        private String comparisonText;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private Boolean success;
        private T data;
        private String message;

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, data, null);
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, data, message);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, null, message);
        }
    }
}