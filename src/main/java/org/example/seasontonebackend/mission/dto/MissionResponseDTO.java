package org.example.seasontonebackend.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.seasontonebackend.mission.domain.entity.MissionCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponseDTO {
    private Long missionId;
    private String title;
    private MissionCategory category;
    private Boolean isActive;
    private LocalDateTime expiresAt;
    private Map<String, Object> questions;  // JSON 그대로 반환
    private Boolean hasParticipated;  // 사용자 참여 여부
}