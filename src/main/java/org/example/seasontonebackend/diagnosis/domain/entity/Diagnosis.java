package org.example.seasontonebackend.diagnosis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "diagnosis",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_latest", columnNames = {"member_id"})
        }
)
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diagnosis_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON", nullable = false)
    private Map<String, Object> scores;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Builder.Default
    @Column(name = "diagnosed_at")
    private LocalDateTime diagnosedAt = LocalDateTime.now();

    public void updateScores(Map<String, Integer> newScores) {
        Map<String, Object> processedScores = new HashMap<>();
        int totalScore = 0;

        for (Map.Entry<String, Integer> entry : newScores.entrySet()) {
            String category = entry.getKey();
            Integer score = entry.getValue();

            if (score < 1 || score > 5) {
                throw new IllegalArgumentException("점수는 1-5 사이여야 합니다: " + category);
            }

            processedScores.put(category, Map.of("score", score));
            totalScore += score;
        }

        this.scores = processedScores;
        this.totalScore = totalScore;
        this.diagnosedAt = LocalDateTime.now();
    }

    @SuppressWarnings("unchecked")
    public Integer getCategoryScore(String category) {
        if (scores == null || !scores.containsKey(category)) {
            return 0;
        }
        Map<String, Object> categoryData = (Map<String, Object>) scores.get(category);
        return (Integer) categoryData.get("score");
    }
}