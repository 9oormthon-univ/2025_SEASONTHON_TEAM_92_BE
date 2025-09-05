package org.example.seasontonebackend.mission.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "mission_participations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_mission",
                        columnNames = {"member_id", "mission_id"}
                )
        }
)
public class MissionParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participation_id")
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON", nullable = false)
    private Map<String, Object> answers;

    @Builder.Default
    @Column(name = "participated_at")
    private LocalDateTime participatedAt = LocalDateTime.now();
}