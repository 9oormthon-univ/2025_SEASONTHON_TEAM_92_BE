package org.example.seasontonebackend.diagnosis.dto;

import lombok.*;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceOptionDTO {
    private Integer score;
    private String text;
    private String context;
    private NumericalValueDTO numericalValue;
}

