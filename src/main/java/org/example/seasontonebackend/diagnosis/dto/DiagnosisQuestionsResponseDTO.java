package org.example.seasontonebackend.diagnosis.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisQuestionsResponseDTO {
    private List<Category> categories;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Category {
        private Long categoryId;
        private Integer sortOrder;
        private List<Question> questions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Question {
        private Long questionId;
        private String questionText;
        private String subText;
        private List<ExperienceOptionDTO> experienceOptions;
    }
}