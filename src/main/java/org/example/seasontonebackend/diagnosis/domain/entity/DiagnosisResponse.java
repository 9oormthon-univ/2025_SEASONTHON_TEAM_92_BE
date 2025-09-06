package org.example.seasontonebackend.diagnosis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.seasontonebackend.diagnosis.domain.DiagnosisScore;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagnosis_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "question_id")
    private Long questionId;

    @Enumerated(EnumType.STRING)
    private DiagnosisScore score;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}