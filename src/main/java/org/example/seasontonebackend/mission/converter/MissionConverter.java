package org.example.seasontonebackend.mission.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.mission.domain.entity.MissionQuestion;
import org.example.seasontonebackend.mission.domain.entity.WeeklyMission;
import org.example.seasontonebackend.mission.dto.MissionResponseDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissionConverter {

    private final ObjectMapper objectMapper;

    public MissionResponseDTO.CurrentMission toCurrentMissionDto(WeeklyMission mission, Integer participationCount, Boolean userParticipated) {
        List<MissionResponseDTO.MissionQuestion> questionDtos = mission.getQuestions().stream()
                .map(this::toMissionQuestionDto)
                .collect(Collectors.toList());

        return MissionResponseDTO.CurrentMission.builder()
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

    public MissionResponseDTO.MissionResult toMissionResultDto(WeeklyMission mission, Integer userScore) {
        // 임시 데이터로 비교 정보 생성 (나중에 실제 계산으로 교체)
        MissionResponseDTO.ComparisonData buildingComparison = MissionResponseDTO.ComparisonData.builder()
                .average(6.2)
                .userRank(3)
                .totalParticipants(12)
                .comparisonText("우리 건물 평균보다 만족도가 높습니다")
                .build();

        MissionResponseDTO.ComparisonData neighborhoodComparison = MissionResponseDTO.ComparisonData.builder()
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

        return MissionResponseDTO.MissionResult.builder()
                .userScore(userScore)
                .maxScore(10) // 최대 점수 (질문 2개 * 5점)
                .category(mission.getCategory())
                .buildingComparison(buildingComparison)
                .neighborhoodComparison(neighborhoodComparison)
                .insights(insights)
                .build();
    }

    // 미션 목록 조회용
    public List<MissionResponseDTO.MissionSummary> toMissionSummaryList(List<WeeklyMission> missions) {
        return missions.stream()
                .map(this::toMissionSummaryDto)
                .collect(Collectors.toList());
    }

    public MissionResponseDTO.MissionSummary toMissionSummaryDto(WeeklyMission mission) {
        return MissionResponseDTO.MissionSummary.builder()
                .missionId(mission.getMissionId())
                .category(mission.getCategory())
                .title(mission.getTitle())
                .startDate(mission.getStartDate())
                .endDate(mission.getEndDate())
                .isActive(mission.getIsActive())
                .build();
    }

    // 미션 상세 조회용
    public MissionResponseDTO.MissionDetail toMissionDetailDto(WeeklyMission mission) {
        List<MissionResponseDTO.MissionQuestion> questionDtos = mission.getQuestions().stream()
                .map(this::toMissionQuestionDto)
                .collect(Collectors.toList());

        return MissionResponseDTO.MissionDetail.builder()
                .missionId(mission.getMissionId())
                .category(mission.getCategory())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .startDate(mission.getStartDate())
                .endDate(mission.getEndDate())
                .isActive(mission.getIsActive())
                .questions(questionDtos)
                .createdAt(mission.getCreatedAt())
                .build();
    }

    private MissionResponseDTO.MissionQuestion toMissionQuestionDto(MissionQuestion question) {
        try {
            List<String> options = objectMapper.readValue(question.getOptions(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

            return MissionResponseDTO.MissionQuestion.builder()
                    .questionId(question.getQuestionId())
                    .questionText(question.getQuestionText())
                    .questionType(question.getQuestionType())
                    .options(options)
                    .orderNumber(question.getOrderNumber())
                    .build();
        } catch (Exception e) {
            log.error("JSON 파싱 오류", e);
            return MissionResponseDTO.MissionQuestion.builder()
                    .questionId(question.getQuestionId())
                    .questionText(question.getQuestionText())
                    .questionType(question.getQuestionType())
                    .options(new ArrayList<>())
                    .orderNumber(question.getOrderNumber())
                    .build();
        }
    }
}