package org.example.seasontonebackend.Mission.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.Mission.converter.DiagnosisConverter;
import org.example.seasontonebackend.Mission.domain.entity.MissionQuestion;
import org.example.seasontonebackend.Mission.domain.entity.UserMissionResponse;
import org.example.seasontonebackend.Mission.domain.entity.WeeklyMission;
import org.example.seasontonebackend.Mission.domain.repository.UserMissionResponseRepository;
import org.example.seasontonebackend.Mission.domain.repository.WeeklyMissionRepository;
import org.example.seasontonebackend.Mission.dto.DiagnosisRequestDTO;
import org.example.seasontonebackend.Mission.dto.DiagnosisResponseDTO;
import org.example.seasontonebackend.Mission.exception.DiagnosisException;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DiagnosisService {

    private final WeeklyMissionRepository missionRepository;
    private final UserMissionResponseRepository responseRepository;
    private final MemberRepository memberRepository;
    private final DiagnosisConverter diagnosisConverter;

    // 현재 활성 미션 조회
    public DiagnosisResponseDTO.CurrentMission getCurrentMission(Long memberId) {
        WeeklyMission mission = missionRepository.findCurrentActiveMission()
                .orElseThrow(() -> new DiagnosisException("현재 활성화된 미션이 없습니다."));

        Integer participationCount = missionRepository.countParticipantsByMissionId(mission.getMissionId());
        Boolean userParticipated = responseRepository.existsByMemberIdAndMissionId(memberId, mission.getMissionId());

        return diagnosisConverter.toCurrentMissionDto(mission, participationCount, userParticipated);
    }

    // 미션 참여하기
    public Long participateInMission(Long memberId, Long missionId, DiagnosisRequestDTO.MissionParticipate request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new DiagnosisException("사용자를 찾을 수 없습니다."));

        WeeklyMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new DiagnosisException("미션을 찾을 수 없습니다."));

        // 이미 참여했는지 확인
        if (responseRepository.existsByMemberIdAndMissionId(memberId, missionId)) {
            throw new DiagnosisException("이미 참여한 미션입니다.");
        }

        Long responseId = null;
        for (DiagnosisRequestDTO.MissionParticipate.Response response : request.getResponses()) {
            MissionQuestion question = mission.getQuestions().stream()
                    .filter(q -> q.getQuestionId().equals(response.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new DiagnosisException("질문을 찾을 수 없습니다."));

            UserMissionResponse userResponse = UserMissionResponse.builder()
                    .member(member)
                    .mission(mission)
                    .question(question)
                    .answer(response.getAnswer())
                    .score(response.getScore())
                    .build();

            UserMissionResponse saved = responseRepository.save(userResponse);
            if (responseId == null) {
                responseId = saved.getResponseId();
            }
        }

        return responseId;
    }

    // 미션 결과 조회
    public DiagnosisResponseDTO.MissionResult getMissionResult(Long memberId, Long missionId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new DiagnosisException("사용자를 찾을 수 없습니다."));

        WeeklyMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new DiagnosisException("미션을 찾을 수 없습니다."));

        // 사용자 점수 조회
        Integer userScore = responseRepository.getTotalScoreByMemberAndMission(memberId, missionId);
        if (userScore == null) {
            throw new DiagnosisException("참여하지 않은 미션입니다.");
        }

        return diagnosisConverter.toMissionResultDto(mission, userScore);
    }
}