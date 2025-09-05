package org.example.seasontonebackend.mission.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.example.seasontonebackend.mission.domain.entity.*;
import org.example.seasontonebackend.mission.domain.repository.MissionParticipationRepository;
import org.example.seasontonebackend.mission.dto.*;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MissionConverter {

    public MissionResponseDTO toResponseDTO(Mission mission, Long memberId) {
        Boolean hasParticipated = null;
        if (memberId != null) {
            hasParticipated = participationRepository.existsByMemberIdAndMissionId(memberId, mission.getId());
        }

        return MissionResponseDTO.builder()
                .missionId(mission.getId())
                .title(mission.getTitle())
                .category(mission.getCategory())
                .isActive(mission.getIsActive())
                .expiresAt(mission.getExpiresAt())
                .questions(mission.getQuestions())
                .hasParticipated(hasParticipated)
                .build();
    }

    private final MissionParticipationRepository participationRepository;

    public MissionResultResponseDTO toResultResponseDTO(Mission mission, MissionParticipationRepository participationRepository) {
        Integer totalParticipants = participationRepository.countParticipantsByMissionId(mission.getId());
        List<MissionParticipation> participations = participationRepository.findByMissionId(mission.getId());

        // JSON에서 questions 추출
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questionsList = (List<Map<String, Object>>) mission.getQuestions().get("questions");

        List<MissionResultResponseDTO.QuestionResultDTO> results = questionsList.stream()
                .map(questionMap -> buildQuestionResult(questionMap, participations))
                .collect(Collectors.toList());

        return MissionResultResponseDTO.builder()
                .missionId(mission.getId())
                .title(mission.getTitle())
                .totalParticipants(totalParticipants)
                .questions(mission.getQuestions())
                .results(results)
                .build();
    }

    @SuppressWarnings("unchecked")
    private MissionResultResponseDTO.QuestionResultDTO buildQuestionResult(
            Map<String, Object> questionMap,
            List<MissionParticipation> participations) {

        Integer questionId = (Integer) questionMap.get("id");
        String questionText = (String) questionMap.get("text");
        List<Map<String, Object>> optionsList = (List<Map<String, Object>>) questionMap.get("options");

        // 이 질문에 대한 모든 답변 수집
        List<Integer> scores = new ArrayList<>();
        Map<Integer, Integer> optionVoteCounts = new HashMap<>();

        for (MissionParticipation participation : participations) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> answers = (List<Map<String, Object>>) participation.getAnswers().get("answers");

            for (Map<String, Object> answer : answers) {
                Integer answerQuestionId = (Integer) answer.get("questionId");
                if (questionId.equals(answerQuestionId)) {
                    Integer optionId = (Integer) answer.get("optionId");
                    Integer score = (Integer) answer.get("score");

                    scores.add(score);
                    optionVoteCounts.put(optionId, optionVoteCounts.getOrDefault(optionId, 0) + 1);
                }
            }
        }

        // 평균 점수 계산
        Double averageScore = scores.isEmpty() ? 0.0 :
                scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        // 옵션별 결과 생성
        List<MissionResultResponseDTO.OptionResultDTO> optionResults = optionsList.stream()
                .map(optionMap -> {
                    Integer optionId = (Integer) optionMap.get("id");
                    String optionText = (String) optionMap.get("text");
                    Integer voteCount = optionVoteCounts.getOrDefault(optionId, 0);
                    Double percentage = participations.isEmpty() ? 0.0 :
                            (voteCount * 100.0) / participations.size();

                    return MissionResultResponseDTO.OptionResultDTO.builder()
                            .optionId(optionId)
                            .optionText(optionText)
                            .voteCount(voteCount)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());

        return MissionResultResponseDTO.QuestionResultDTO.builder()
                .questionId(questionId)
                .questionText(questionText)
                .averageScore(averageScore)
                .optionResults(optionResults)
                .build();
    }
}