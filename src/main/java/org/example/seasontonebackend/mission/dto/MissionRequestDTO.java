package org.example.seasontonebackend.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.seasontonebackend.mission.domain.entity.MissionCategory;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MissionRequestDTO {
    private String title;
    private MissionCategory category;
    private LocalDateTime expiresAt;
    private List<QuestionDTO> questions;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDTO {
        private Integer id;
        private String text;
        private String type;  // MULTIPLE_CHOICE, SCALE_1_5
        private List<OptionDTO> options;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDTO {
        private Integer id;
        private String text;
        private Integer score;
    }
}