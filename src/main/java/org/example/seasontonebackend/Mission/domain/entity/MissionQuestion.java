package org.example.seasontonebackend.Mission.domain.entity;

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
}