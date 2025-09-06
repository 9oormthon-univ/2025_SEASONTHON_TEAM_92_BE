package org.example.seasontonebackend.mission.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.seasontonebackend.member.domain.Member;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_mission_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMissionResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long responseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private WeeklyMission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private MissionQuestion question;

    @Column(nullable = false)
    private String answer;

    @Column(nullable = false)
    private Integer score;

    @CreationTimestamp
    private LocalDateTime submittedAt;
}