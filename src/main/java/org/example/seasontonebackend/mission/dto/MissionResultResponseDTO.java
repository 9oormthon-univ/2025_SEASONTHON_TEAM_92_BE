package org.example.seasontonebackend.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionResultResponseDTO {
    private Long missionId;
    private String title;
    private Integer totalParticipants;
    private Map<String, Object> questions;  // 원본 질문 데이터
    private List<QuestionResultDTO> results;  // 질문별 결과

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResultDTO {
        private Integer questionId;
        private String questionText;
        private Double averageScore;
        private List<OptionResultDTO> optionResults;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionResultDTO {
        private Integer optionId;
        private String optionText;
        private Integer voteCount;
        private Double percentage;
    }
}