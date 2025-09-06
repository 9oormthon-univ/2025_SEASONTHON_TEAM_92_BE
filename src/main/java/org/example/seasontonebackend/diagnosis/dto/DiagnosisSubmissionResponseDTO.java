package org.example.seasontonebackend.diagnosis.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisSubmissionResponseDTO {
    private Integer totalScore;
    private Integer maxScore;
    private Integer responseCount;
    private LocalDateTime submittedAt;
}