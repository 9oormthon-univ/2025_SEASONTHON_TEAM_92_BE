package org.example.seasontonebackend.mission.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.seasontonebackend.mission.converter.MissionConverter;
import org.example.seasontonebackend.mission.domain.entity.*;
import org.example.seasontonebackend.mission.domain.repository.*;
import org.example.seasontonebackend.mission.dto.*;
import org.example.seasontonebackend.mission.exception.MissionException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MissionServiceImpl implements MissionService {

    private final MissionRepository missionRepository;
    private final MissionParticipationRepository participationRepository;
    private final MissionConverter missionConverter;

    @Override
    public MissionResponseDTO createMission(MissionRequestDTO request, Long adminId) {
        Mission mission = Mission.builder()
                .title(request.getTitle())
                .category(request.getCategory())
                .questions(convertQuestionsToMap(request.getQuestions()))
                .expiresAt(request.getExpiresAt())
                .createdBy(adminId)
                .build();

        Mission savedMission = missionRepository.save(mission);
        return missionConverter.toResponseDTO(savedMission, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponseDTO> getActiveMissions(Long memberId) {
        List<Mission> missions = missionRepository.findByIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(
                LocalDateTime.now());

        return missions.stream()
                .map(mission -> missionConverter.toResponseDTO(mission, memberId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponseDTO> getAllMissions() {
        List<Mission> missions = missionRepository.findAllByOrderByCreatedAtDesc();
        return missions.stream()
                .map(mission -> missionConverter.toResponseDTO(mission, null))
                .collect(Collectors.toList());
    }

    @Override
    public void participate(Long missionId, Long memberId, ParticipateRequestDTO request) {
        // 중복 참여 확인
        if (participationRepository.existsByMemberIdAndMissionId(memberId, missionId)) {
            throw new MissionException("이미 참여한 미션입니다.");
        }

        // 미션 유효성 확인
        Mission mission = missionRepository.findByIdAndIsActiveTrueAndExpiresAtAfter(
                        missionId, LocalDateTime.now())
                .orElseThrow(() -> new MissionException("유효하지 않은 미션입니다."));

        // 참여 데이터 저장
        Map<String, Object> answersMap = convertAnswersToMap(request.getAnswers());
        MissionParticipation participation = MissionParticipation.builder()
                .missionId(missionId)
                .memberId(memberId)
                .answers(answersMap)
                .build();

        participationRepository.save(participation);
    }

    @Override
    @Transactional(readOnly = true)
    public MissionResultResponseDTO getMissionResults(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException("미션을 찾을 수 없습니다."));

        return missionConverter.toResultResponseDTO(mission, participationRepository);
    }

    @Override
    public void updateMissionStatus(Long missionId, Boolean isActive) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException("미션을 찾을 수 없습니다."));

        mission.updateStatus(isActive);
        missionRepository.save(mission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponseDTO> getMyParticipations(Long memberId) {
        List<Long> participatedMissionIds = participationRepository.findDistinctMissionIdsByMemberId(memberId);
        List<Mission> missions = missionRepository.findAllById(participatedMissionIds);

        return missions.stream()
                .map(mission -> missionConverter.toResponseDTO(mission, memberId))
                .collect(Collectors.toList());
    }

    // Helper methods
    private Map<String, Object> convertQuestionsToMap(List<MissionRequestDTO.QuestionDTO> questions) {
        Map<String, Object> questionsMap = new HashMap<>();
        questionsMap.put("questions", questions);
        return questionsMap;
    }

    private Map<String, Object> convertAnswersToMap(List<ParticipateRequestDTO.AnswerDTO> answers) {
        Map<String, Object> answersMap = new HashMap<>();
        answersMap.put("answers", answers);
        return answersMap;
    }
}