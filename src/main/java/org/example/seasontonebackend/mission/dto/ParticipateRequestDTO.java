package org.example.seasontonebackend.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipateRequestDTO {
    private List<AnswerDTO> answers;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDTO {
        private Integer questionId;
        private Integer optionId;
        private Integer score;
    }
}