package org.example.seasontonebackend.diagnosis.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.diagnosis.domain.entity.DiagnosisResponse;
import org.example.seasontonebackend.diagnosis.domain.repository.DiagnosisResponseRepository;
import org.example.seasontonebackend.diagnosis.dto.*;
import org.example.seasontonebackend.diagnosis.domain.DiagnosisScore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisResponseRepository responseRepository;

    public DiagnosisQuestionsResponseDTO getQuestions() {
        List<DiagnosisQuestionsResponseDTO.Category> categories = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            List<DiagnosisQuestionsResponseDTO.Question> questions = new ArrayList<>();

            for (int j = 1; j <= 2; j++) {
                DiagnosisQuestionsResponseDTO.Question question = DiagnosisQuestionsResponseDTO.Question.builder()
                        .questionId((long) ((i - 1) * 2 + j))
                        .questionText(getQuestionText(i, j))
                        .subText(getSubText(i, j))
                        .build();
                questions.add(question);
            }

            DiagnosisQuestionsResponseDTO.Category category = DiagnosisQuestionsResponseDTO.Category.builder()
                    .categoryId((long) i)
                    .sortOrder(i)
                    .title(getCategoryTitle(i))
                    .description(getCategoryDescription(i))
                    .questions(questions)
                    .build();

            categories.add(category);
        }

        return DiagnosisQuestionsResponseDTO.builder()
                .categories(categories)
                .build();
    }

    @Transactional
    public DiagnosisSubmissionResponseDTO submitResponses(Member member, DiagnosisRequestDTO request) {
        responseRepository.deleteByUserId(member.getId());

        int totalScore = 0;

        for (DiagnosisRequestDTO.Response responseItem : request.getResponses()) {
            DiagnosisScore score = DiagnosisScore.fromValue(responseItem.getScore());

            DiagnosisResponse response = DiagnosisResponse.builder()
                    .userId(member.getId())
                    .questionId(responseItem.getQuestionId())
                    .score(score)
                    .createdAt(LocalDateTime.now())
                    .build();

            responseRepository.save(response);
            totalScore += score.getIntValue();
        }

        return DiagnosisSubmissionResponseDTO.builder()
                .totalScore(totalScore)
                .maxScore(100)
                .responseCount(request.getResponses().size())
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public DiagnosisSubmissionResponseDTO submitBulkResponses(Member member, List<DiagnosisRequestDTO> requests) {
        responseRepository.deleteByUserId(member.getId());

        int totalScore = 0;
        int totalResponses = 0;

        for (DiagnosisRequestDTO request : requests) {
            for (DiagnosisRequestDTO.Response responseItem : request.getResponses()) {
                DiagnosisScore score = DiagnosisScore.fromValue(responseItem.getScore());

                DiagnosisResponse response = DiagnosisResponse.builder()
                        .userId(member.getId())
                        .questionId(responseItem.getQuestionId())
                        .score(score)
                        .createdAt(LocalDateTime.now())
                        .build();

                responseRepository.save(response);
                totalScore += score.getIntValue();
                totalResponses++;
            }
        }

        return DiagnosisSubmissionResponseDTO.builder()
                .totalScore(totalScore)
                .maxScore(100)
                .responseCount(totalResponses)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    public DiagnosisResultResponseDTO getResult(Member member) {
        List<DiagnosisResponse> responses = responseRepository.findByUserId(member.getId());

        if (responses.isEmpty()) {
            throw new RuntimeException("진단 결과가 없습니다.");
        }

        int totalScore = responses.stream()
                .mapToInt(r -> r.getScore().getIntValue())
                .sum();

        String grade = calculateGrade(totalScore);

        List<DiagnosisResultResponseDTO.CategoryDetail> categoryDetails = new ArrayList<>();
        for (long categoryId = 1; categoryId <= 10; categoryId++) {
            double myScore = calculateCategoryScore(responses, categoryId);

            DiagnosisResultResponseDTO.CategoryDetail detail = DiagnosisResultResponseDTO.CategoryDetail.builder()
                    .categoryId(categoryId)
                    .myScore(myScore)
                    .buildingAverage(myScore + (Math.random() - 0.5))
                    .neighborhoodAverage(myScore + (Math.random() - 0.5))
                    .build();

            categoryDetails.add(detail);
        }

        DiagnosisResultResponseDTO.Analysis analysis = DiagnosisResultResponseDTO.Analysis.builder()
                .strengths(getTopCategories(categoryDetails, true))
                .improvements(getTopCategories(categoryDetails, false))
                .build();

        DiagnosisResultResponseDTO.Statistics statistics = DiagnosisResultResponseDTO.Statistics.builder()
                .participantCount(8)
                .responseCount(156)
                .buildingResidents(8)
                .neighborhoodResidents(6)
                .build();

        DiagnosisResultResponseDTO.Summary summary = DiagnosisResultResponseDTO.Summary.builder()
                .totalScore(totalScore)
                .grade(grade)
                .buildingAverage(68.0)
                .neighborhoodAverage(71.0)
                .buildingRank(3)
                .neighborhoodRank(8)
                .build();

        return DiagnosisResultResponseDTO.builder()
                .summary(summary)
                .categoryDetails(categoryDetails)
                .analysis(analysis)
                .statistics(statistics)
                .build();
    }

    private String calculateGrade(int totalScore) {
        if (totalScore >= 80) return "우수";
        else if (totalScore >= 60) return "양호";
        else if (totalScore >= 40) return "보통";
        else return "개선 필요";
    }

    private double calculateCategoryScore(List<DiagnosisResponse> responses, long categoryId) {
        return responses.stream()
                .filter(r -> getCategoryId(r.getQuestionId()) == categoryId)
                .mapToInt(r -> r.getScore().getIntValue())
                .average()
                .orElse(0.0);
    }

    private long getCategoryId(long questionId) {
        return (questionId - 1) / 2 + 1;
    }

    private List<DiagnosisResultResponseDTO.AnalysisItem> getTopCategories(
            List<DiagnosisResultResponseDTO.CategoryDetail> categoryDetails, boolean isStrength) {
        return categoryDetails.stream()
                .sorted((a, b) -> {
                    double scoreA = a.getMyScore();
                    double scoreB = b.getMyScore();
                    return isStrength ? Double.compare(scoreB, scoreA) : Double.compare(scoreA, scoreB);
                })
                .limit(3)
                .map(detail -> DiagnosisResultResponseDTO.AnalysisItem.builder()
                        .categoryId(detail.getCategoryId())
                        .score((int) (detail.getMyScore() * 20))
                        .build())
                .collect(Collectors.toList());
    }

    private String getQuestionText(int categoryId, int questionOrder) {
        String[][] questions = {
                {"옆집/윗집 생활소음이 어느 정도인가요?", "외부 소음(교통, 공사 등)은 어떤가요?"},
                {"샤워할 때 수압은 어떤가요?", "온수가 나오는 속도는 어떤가요?"},
                {"낮 시간 자연광은 충분한가요?", "하루 중 햇빛이 드는 시간은?"},
                {"주차공간 확보는 어떤가요?", "집까지의 거리는 어떤가요?"},
                {"겨울철 난방 효율은 어떤가요?", "난방비 부담은 어떤가요?"},
                {"실내 공기순환은 어떤가요?", "습도 조절은 어떤가요?"},
                {"건물 보안시설은 어떤가요?", "밤시간 안전함은 어떤가요?"},
                {"건물 관리상태는 어떤가요?", "수리 요청 시 대응속도는?"},
                {"주변 편의시설은 어떤가요?", "대중교통 접근성은 어떤가요?"},
                {"인터넷 속도는 어떤가요?", "WiFi 안정성은 어떤가요?"}
        };
        return questions[categoryId - 1][questionOrder - 1];
    }

    private String getSubText(int categoryId, int questionOrder) {
        String[][] subTexts = {
                {"매우 조용함~매우 시끄러움", "전혀 안 들림~매우 시끄러움"},
                {"매우 약함~매우 강함", "매우 늦음~매우 빠름"},
                {"매우 어두움~매우 밝음", "거의 없음~하루종일"},
                {"매우 어려움~매우 쉬움", "매우 멀음~매우 가까움"},
                {"매우 나쁨~매우 좋음", "매우 부담~전혀 부담 없음"},
                {"매우 나쁨~매우 좋음", "매우 어려움~매우 쉬움"},
                {"매우 미흡~매우 완벽", "매우 불안~매우 안전"},
                {"매우 나쁨~매우 좋음", "매우 느림~매우 빠름"},
                {"매우 부족함~매우 충분함", "매우 불편함~매우 편리함"},
                {"매우 느림~매우 빠름", "매우 불안정함~매우 안정함"}
        };
        return subTexts[categoryId - 1][questionOrder - 1];
    }

    private String getCategoryTitle(int categoryId) {
        String[] titles = {
            "소음", "수압", "채광", "주차", "난방",
            "환기", "보안", "관리", "편의성", "인터넷"
        };
        return titles[categoryId - 1];
    }

    private String getCategoryDescription(int categoryId) {
        String[] descriptions = {
            "이웃 소음과 외부 소음 정도",
            "물의 압력과 온수 공급",
            "자연광과 햇빛 유입",
            "주차 공간 확보와 접근성",
            "겨울철 난방 효율과 비용",
            "실내 공기순환과 습도 조절",
            "건물 보안시설과 밤시간 안전함",
            "건물 관리상태와 수리 요청 시 대응 속도",
            "주변 편의시설과 대중교통 접근성",
            "인터넷 속도와 WiFi 안정성"
        };
        return descriptions[categoryId - 1];
    }
}