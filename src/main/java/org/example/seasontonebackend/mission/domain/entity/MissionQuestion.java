package org.example.seasontonebackend.mission.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mission_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private WeeklyMission mission;

    @Column(nullable = false)
    private String questionText;

    @Column(nullable = false)
    private String questionType;

    @Column(columnDefinition = "TEXT")
    private String options; // JSON 형태로 저장

    @Column(nullable = false)
    private Integer orderNumber;

    // Lombok이 제대로 동작하지 않을 경우를 위한 명시적 getter 메소드들
    public Long getQuestionId() {
        return questionId;
    }

    public WeeklyMission getMission() {
        return mission;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getOptions() {
        return options;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }
}