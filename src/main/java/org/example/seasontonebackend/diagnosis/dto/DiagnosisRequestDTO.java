package org.example.seasontonebackend.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRequestDTO {
    private Map<String, Integer> scores; // 카테고리 -> 1-5점

    public Integer getTotalScore() {
        return scores.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
}