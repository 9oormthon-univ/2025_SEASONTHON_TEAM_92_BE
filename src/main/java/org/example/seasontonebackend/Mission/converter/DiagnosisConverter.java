package org.example.seasontonebackend.Mission.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.Mission.domain.entity.MissionQuestion;
import org.example.seasontonebackend.Mission.domain.entity.WeeklyMission;
import org.example.seasontonebackend.Mission.dto.DiagnosisResponseDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiagnosisConverter {

    private final ObjectMapper objectMapper;

    public DiagnosisResponseDTO.CurrentMission toCurrentMissionDto(WeeklyMission mission, Integer participationCount, Boolean userParticipated) {
        List<DiagnosisResponseDTO.MissionQuestion> questionDtos = mission.getQuestions().stream()
                .map(this::toMissionQuestionDto)
                .collect(Collectors.toList());

        return DiagnosisResponseDTO.CurrentMission.builder()
                .missionId(mission.getMissionId())
                .category(mission.getCategory())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .startDate(mission.getStartDate())
                .endDate(mission.getEndDate())
                .questions(questionDtos)
                .participationCount(participationCount)
                .userParticipated(userParticipated)
                .build();
    }

    public DiagnosisResponseDTO.MissionResult toMissionResultDto(WeeklyMission mission, Integer userScore) {
        // 임시 데이터로 비교 정보 생성 (나중에 실제 계산으로 교체)
        DiagnosisResponseDTO.ComparisonData buildingComparison = DiagnosisResponseDTO.ComparisonData.builder()
                .average(6.2)
                .userRank(3)
                .totalParticipants(12)
                .comparisonText("우리 건물 평균보다 만족도가 높습니다")
                .build();

        DiagnosisResponseDTO.ComparisonData neighborhoodComparison = DiagnosisResponseDTO.ComparisonData.builder()
                .average(5.8)
                .userRank(8)
                .totalParticipants(45)
                .comparisonText("우리 동네 평균보다 만족도가 높습니다")
                .build();

        // 인사이트 생성
        List<String> insights = List.of(
                "우리 건물은 " + mission.getCategory() + " 환경이 만족스러운 편입니다",
                "87%의 참가자가 " + mission.getCategory() + "에 만족하고 있습니다"
        );

        return DiagnosisResponseDTO.MissionResult.builder()
                .userScore(userScore)
                .maxScore(10) // 최대 점수 (질문 2개 * 5점)
                .category(mission.getCategory())
                .buildingComparison(buildingComparison)
                .neighborhoodComparison(neighborhoodComparison)
                .insights(insights)
                .build();
    }

    private DiagnosisResponseDTO.MissionQuestion toMissionQuestionDto(MissionQuestion question) {
        try {
            List<String> options = objectMapper.readValue(question.getOptions(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

            return DiagnosisResponseDTO.MissionQuestion.builder()
                    .questionId(question.getQuestionId())
                    .questionText(question.getQuestionText())
                    .questionType(question.getQuestionType())
                    .options(options)
                    .orderNumber(question.getOrderNumber())
                    .build();
        } catch (Exception e) {
            log.error("JSON 파싱 오류", e);
            return DiagnosisResponseDTO.MissionQuestion.builder()
                    .questionId(question.getQuestionId())
                    .questionText(question.getQuestionText())
                    .questionType(question.getQuestionType())
                    .options(new ArrayList<>())
                    .orderNumber(question.getOrderNumber())
                    .build();
        }
    }
}