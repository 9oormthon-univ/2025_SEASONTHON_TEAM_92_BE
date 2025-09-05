package org.example.seasontonebackend.mission.application;

import org.example.seasontonebackend.mission.dto.*;
import java.util.List;

public interface MissionService {
    MissionResponseDTO createMission(MissionRequestDTO request, Long adminId);
    List<MissionResponseDTO> getActiveMissions(Long memberId);
    List<MissionResponseDTO> getAllMissions();
    void participate(Long missionId, Long memberId, ParticipateRequestDTO request);
    MissionResultResponseDTO getMissionResults(Long missionId);
    void updateMissionStatus(Long missionId, Boolean isActive);
    List<MissionResponseDTO> getMyParticipations(Long memberId);
}