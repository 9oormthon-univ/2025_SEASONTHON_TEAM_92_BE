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
                        .experienceOptions(getExperienceOptions(i, j))
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
            // 진단 결과가 없는 경우 기본값 반환
            return DiagnosisResultResponseDTO.builder()
                    .summary(DiagnosisResultResponseDTO.Summary.builder()
                            .totalScore(0)
                            .grade("미완료")
                            .buildingAverage(0.0)
                            .neighborhoodAverage(0.0)
                            .buildingRank(0)
                            .neighborhoodRank(0)
                            .build())
                    .categoryDetails(new ArrayList<>())
                    .analysis(DiagnosisResultResponseDTO.Analysis.builder()
                            .strengths(new ArrayList<>())
                            .improvements(new ArrayList<>())
                            .build())
                    .statistics(DiagnosisResultResponseDTO.Statistics.builder()
                            .participantCount(0)
                            .responseCount(0)
                            .buildingResidents(0)
                            .neighborhoodResidents(0)
                            .build())
                    .build();
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
                {"평상시 옆집에서 나는 소리 정도는?", "창문 닫은 상태에서 외부 소음 정도는?"},
                {"샤워할 때 느끼는 물줄기 굵기는?", "수도꼭지 틀고 온수 나올 때까지 시간은?"},
                {"오후 2-3시 거실 밝기 정도는?", "하루 중 햇빛 드는 시간은?"},
                {"평소 주차 가능성은?", "주차 후 집까지 걷는 거리는?"},
                {"겨울철 실내 온도와 난방비 종합은?", "난방비 부담 정도는?"},
                {"실내 공기 상태는?", "환기 효과는?"},
                {"건물 보안시설 수준은?", "밤시간 동네 안전도는?"},
                {"건물 청소와 관리 상태는?", "수리 요청시 대응 속도는?"},
                {"생활 편의시설까지 거리는?", "대중교통 접근성은?"},
                {"인터넷 사용시 체감 속도는?", "WiFi 연결 안정성은?"}
        };
        return questions[categoryId - 1][questionOrder - 1];
    }

    private String getSubText(int categoryId, int questionOrder) {
        String[][] subTexts = {
                {"이웃 생활소음 수준을 평가해주세요", "외부 교통소음 수준을 평가해주세요"},
                {"샤워 시 물압 상태를 평가해주세요", "온수 공급 속도를 평가해주세요"},
                {"자연광 밝기를 평가해주세요", "일조시간을 평가해주세요"},
                {"주차 공간 확보 가능성을 평가해주세요", "주차장에서 집까지 거리를 평가해주세요"},
                {"실내 난방 효과를 평가해주세요", "난방비 부담을 평가해주세요"},
                {"실내 공기질을 평가해주세요", "환기 효과를 평가해주세요"},
                {"건물 보안 수준을 평가해주세요", "야간 안전도를 평가해주세요"},
                {"건물 관리 상태를 평가해주세요", "수리 대응 속도를 평가해주세요"},
                {"편의시설 접근성을 평가해주세요", "대중교통 접근성을 평가해주세요"},
                {"인터넷 속도를 평가해주세요", "WiFi 안정성을 평가해주세요"}
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

    private String getMeasurementContext(int categoryId, int questionOrder) {
        String[][] contexts = {
                {"평상시 옆집에서 나는 소리 정도", "창문 닫은 상태에서 외부 소음 정도"}, // 소음
                {"샤워할 때 느끼는 물줄기 굵기", "수도꼭지 틀고 온수 나올 때까지 시간"}, // 수압
                {"오후 2-3시 거실 밝기 정도", "하루 중 햇빛 드는 시간"}, // 채광
                {"평소 주차 가능성", "주차 후 집까지 걷는 거리"}, // 주차
                {"겨울철 실내 온도와 난방비 종합", "난방비 부담 정도"}, // 난방
                {"실내 공기 상태", "환기 효과"}, // 환기
                {"건물 보안시설 수준", "밤시간 동네 안전도"}, // 보안
                {"건물 청소와 관리 상태", "수리 요청시 대응 속도"}, // 관리
                {"생활 편의시설까지 거리", "대중교통 접근성"}, // 편의시설
                {"인터넷 사용시 체감 속도", "WiFi 연결 안정성"} // 인터넷
        };

        if (categoryId >= 1 && categoryId <= 10 && questionOrder >= 1 && questionOrder <= 2) {
            return contexts[categoryId-1][questionOrder-1];
        }
        return "해당 항목에 대한 경험을 선택해주세요";
    }

    private List<ExperienceOptionDTO> getExperienceOptions(int categoryId, int questionOrder) {

        // === 1. 소음 카테고리 ===
        if (categoryId == 1 && questionOrder == 1) { // 생활소음
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("옆집 TV 드라마 대사가 선명하게 들림").context("우리 집 TV 끄고 있어도 들림").build(),
                    ExperienceOptionDTO.builder().score(2).text("의자 끄는 소리나 문 닫는 소리가 자주 들림").context("옆집 움직임이 다 들림").build(),
                    ExperienceOptionDTO.builder().score(3).text("발소리가 가끔 들림").context("조용할 때만 희미하게").build(),
                    ExperienceOptionDTO.builder().score(4).text("아주 조용할 때만 희미하게 들림").context("의식해야 들림").build(),
                    ExperienceOptionDTO.builder().score(5).text("전혀 들리지 않음").context("완전히 조용").build()
            );
        }
        if (categoryId == 1 && questionOrder == 2) { // 외부소음
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("창문 닫아도 트럭 소리가 크게 들림").context("대화 중단할 정도로 시끄러움").build(),
                    ExperienceOptionDTO.builder().score(2).text("큰 차량 지나갈 때마다 소리 남").context("창문 열면 시끄러움").build(),
                    ExperienceOptionDTO.builder().score(3).text("일반 승용차 소리가 배경음처럼 들림").context("있지만 신경 안 씀").build(),
                    ExperienceOptionDTO.builder().score(4).text("가끔 차 지나가는 소리가 희미하게 들림").context("조용한 주택가 수준").build(),
                    ExperienceOptionDTO.builder().score(5).text("거의 들리지 않아서 조용함").context("완전 조용한 환경").build()
            );
        }

        // === 2. 수압 카테고리 ===
        if (categoryId == 2 && questionOrder == 1) { // 샤워 수압
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("물이 실처럼 가늘게 떨어짐").context("샤워 답답하고 시간 오래 걸림").build(),
                    ExperienceOptionDTO.builder().score(2).text("젓가락 굵기 정도로 나옴").context("샤워되지만 시원하지 않음").build(),
                    ExperienceOptionDTO.builder().score(3).text("엄지손가락 굵기로 나옴").context("무난한 샤워 가능").build(),
                    ExperienceOptionDTO.builder().score(4).text("500원 동전 크기로 퍼져 나옴").context("시원하고 쾌적함").build(),
                    ExperienceOptionDTO.builder().score(5).text("넓고 강한 물줄기가 나옴").context("마사지 느낌").build()
            );
        }
        if (categoryId == 2 && questionOrder == 2) { // 온수 속도
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("3분 이상 기다려야 온수 나옴").context("라면 끓일 시간").build(),
                    ExperienceOptionDTO.builder().score(2).text("2분 정도 기다려야 함").context("커피 한 잔 마실 시간").build(),
                    ExperienceOptionDTO.builder().score(3).text("1분 정도 기다리면 나옴").context("휴대폰 메시지 확인할 시간").build(),
                    ExperienceOptionDTO.builder().score(4).text("30초 정도면 온수 나옴").context("양치컵에 물 받는 시간").build(),
                    ExperienceOptionDTO.builder().score(5).text("바로 따뜻한 물 나옴").context("수도꼭지 틀자마자").build()
            );
        }

        // === 3. 채광 카테고리 ===
        if (categoryId == 3 && questionOrder == 1) { // 자연광
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("낮에도 휴대폰 화면이 어둡게 보임").context("오후에도 전등 필요").build(),
                    ExperienceOptionDTO.builder().score(2).text("신문 읽으려면 눈을 찡그려야 함").context("흐린 날 전등 필요").build(),
                    ExperienceOptionDTO.builder().score(3).text("책 읽기는 되지만 조금 어두움").context("보통 수준의 밝기").build(),
                    ExperienceOptionDTO.builder().score(4).text("글씨 쓰기 딱 좋은 밝기").context("밝고 쾌적함").build(),
                    ExperienceOptionDTO.builder().score(5).text("너무 밝아서 커튼을 쳐야 함").context("매우 밝은 자연광").build()
            );
        }
        if (categoryId == 3 && questionOrder == 2) { // 일조시간
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("아침 식사 시간 정도만 잠깐 햇빛").context("2시간 미만").build(),
                    ExperienceOptionDTO.builder().score(2).text("점심시간 전후로만 햇빛").context("2-4시간 정도").build(),
                    ExperienceOptionDTO.builder().score(3).text("오전부터 오후까지 적당히 햇빛").context("4-6시간 정도").build(),
                    ExperienceOptionDTO.builder().score(4).text("아침부터 저녁까지 계속 밝음").context("6-8시간 정도").build(),
                    ExperienceOptionDTO.builder().score(5).text("해 뜰 때부터 질 때까지 하루 종일").context("8시간 이상").build()
            );
        }

        // === 4. 주차 카테고리 ===
        if (categoryId == 4 && questionOrder == 1) { // 주차 가능성
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("블록 여러 개 돌아야 주차 가능").context("학교 운동장 한 바퀴 거리").build(),
                    ExperienceOptionDTO.builder().score(2).text("한 블록 정도 돌아야 함").context("편의점 2-3개 지나는 거리").build(),
                    ExperienceOptionDTO.builder().score(3).text("집 주변에서 조금 찾으면 가능").context("집 앞 골목 한두 바퀴").build(),
                    ExperienceOptionDTO.builder().score(4).text("대부분 쉽게 주차 가능").context("집에서 마트까지 거리").build(),
                    ExperienceOptionDTO.builder().score(5).text("항상 바로 주차 가능").context("집 대문 바로 앞").build()
            );
        }
        if (categoryId == 4 && questionOrder == 2) { // 주차장-집 거리
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("걸어서 5분 이상").context("마트 쇼핑하고 돌아올 거리").build(),
                    ExperienceOptionDTO.builder().score(2).text("걸어서 3분 정도").context("편의점 갔다 올 거리").build(),
                    ExperienceOptionDTO.builder().score(3).text("걸어서 1-2분").context("빵집에서 빵 사올 거리").build(),
                    ExperienceOptionDTO.builder().score(4).text("30초 정도").context("계단 몇 개 층 올라가는 정도").build(),
                    ExperienceOptionDTO.builder().score(5).text("바로 앞").context("차에서 내리자마자 집 현관 보임").build()
            );
        }

        // === 5. 난방 카테고리 ===
        if (categoryId == 5 && questionOrder == 1) { // 실내온도
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("패딩 입고 있어야 함").context("실내에서도 입김 날 정도로 추움").build(),
                    ExperienceOptionDTO.builder().score(2).text("두꺼운 가디건 필수").context("담요 없이는 생활하기 힘듦").build(),
                    ExperienceOptionDTO.builder().score(3).text("긴팔 티셔츠에 얇은 가디건").context("보통 수준의 실내 온도").build(),
                    ExperienceOptionDTO.builder().score(4).text("긴팔 티셔츠만 입어도 따뜻함").context("쾌적한 실내 온도").build(),
                    ExperienceOptionDTO.builder().score(5).text("반팔도 괜찮을 정도").context("매우 따뜻한 실내").build()
            );
        }
        if (categoryId == 5 && questionOrder == 2) { // 난방비
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("월 20만원 이상").context("명품 가방 살 돈").build(),
                    ExperienceOptionDTO.builder().score(2).text("월 15만원 정도").context("가족 외식비 한 달치").build(),
                    ExperienceOptionDTO.builder().score(3).text("월 10만원 정도").context("통신비 정도").build(),
                    ExperienceOptionDTO.builder().score(4).text("월 7만원 정도").context("배달음식 몇 번 시킨 정도").build(),
                    ExperienceOptionDTO.builder().score(5).text("월 5만원 미만").context("카페 음료값 정도로 거의 안 나옴").build()
            );
        }

        // === 6. 환기/습도 카테고리 ===
        if (categoryId == 6 && questionOrder == 1) { // 공기순환
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("항상 습하고 답답함").context("찜질방처럼 후덥지근함").build(),
                    ExperienceOptionDTO.builder().score(2).text("빨래 널어놓은 것처럼 눅눅함").context("장마철 실내 같은 느낌").build(),
                    ExperienceOptionDTO.builder().score(3).text("여름날 같은 느낌").context("에어컨 없는 여름 실내").build(),
                    ExperienceOptionDTO.builder().score(4).text("봄날처럼 상쾌함").context("좋은 날씨 야외 수준").build(),
                    ExperienceOptionDTO.builder().score(5).text("산속처럼 맑고 시원함").context("고산지대 맑은 공기").build()
            );
        }
        if (categoryId == 6 && questionOrder == 2) { // 습도조절
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("벽에 물방울이 맺힘").context("곰팡이 생기고 결로 심함").build(),
                    ExperienceOptionDTO.builder().score(2).text("거울이 자주 김 서는 정도").context("습기 제거제 필수").build(),
                    ExperienceOptionDTO.builder().score(3).text("장마철 같지만 참을 만함").context("조금 습하지만 생활 가능").build(),
                    ExperienceOptionDTO.builder().score(4).text("에어컨 켠 것처럼 쾌적함").context("적정 습도로 편안함").build(),
                    ExperienceOptionDTO.builder().score(5).text("호텔처럼 완벽하게 관리됨").context("최적의 습도 환경").build()
            );
        }

        // === 7. 보안/안전 카테고리 ===
        if (categoryId == 7 && questionOrder == 1) { // 건물보안
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("아무나 들어올 수 있음").context("공원 화장실처럼 출입 자유").build(),
                    ExperienceOptionDTO.builder().score(2).text("출입문만 있고 관리 안됨").context("편의점처럼 형식적 출입문").build(),
                    ExperienceOptionDTO.builder().score(3).text("출입카드로 들어감").context("은행처럼 출입통제 있음").build(),
                    ExperienceOptionDTO.builder().score(4).text("경비실 있고 관리 잘됨").context("회사 사무실처럼 체계적 보안").build(),
                    ExperienceOptionDTO.builder().score(5).text("24시간 경비 완벽 보안").context("최고 수준의 보안 환경").build()
            );
        }
        if (categoryId == 7 && questionOrder == 2) { // 야간안전
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("가로등 별로 없고 어두움").context("깜깜한 시골길 수준").build(),
                    ExperienceOptionDTO.builder().score(2).text("조금 으스스한 느낌").context("공원 뒷길 같은 분위기").build(),
                    ExperienceOptionDTO.builder().score(3).text("적당히 안전함").context("일반 주택가 수준").build(),
                    ExperienceOptionDTO.builder().score(4).text("밝고 사람 많아서 안전").context("번화가처럼 안전함").build(),
                    ExperienceOptionDTO.builder().score(5).text("완벽하게 안전함").context("대사관가처럼 최고 수준").build()
            );
        }

        // === 8. 건물관리 카테고리 ===
        if (categoryId == 8 && questionOrder == 1) { // 관리상태
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("계단에 먼지 쌓이고 더러움").context("폐건물처럼 방치 상태").build(),
                    ExperienceOptionDTO.builder().score(2).text("청소하지만 낡고 지저분함").context("오래된 학교처럼 관리 부족").build(),
                    ExperienceOptionDTO.builder().score(3).text("기본적인 관리는 됨").context("일반 아파트처럼 정기 청소").build(),
                    ExperienceOptionDTO.builder().score(4).text("깨끗하고 관리 잘됨").context("백화점처럼 철저한 관리").build(),
                    ExperienceOptionDTO.builder().score(5).text("항상 완벽하게 관리됨").context("5성급 호텔처럼 24시간 관리").build()
            );
        }
        if (categoryId == 8 && questionOrder == 2) { // 수리대응
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("일주일 넘게 걸림").context("연락조차 안 옴").build(),
                    ExperienceOptionDTO.builder().score(2).text("3-5일 걸림").context("택배 배송처럼 느림").build(),
                    ExperienceOptionDTO.builder().score(3).text("1-2일 걸림").context("온라인 쇼핑몰 교환 수준").build(),
                    ExperienceOptionDTO.builder().score(4).text("당일이나 다음날 처리").context("피자 배달처럼 신속함").build(),
                    ExperienceOptionDTO.builder().score(5).text("몇 시간 내 즉시 해결").context("응급실처럼 즉시 대응").build()
            );
        }

        // === 9. 편의시설 카테고리 ===
        if (categoryId == 9 && questionOrder == 1) { // 편의시설
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("마트 가려면 버스 타고 가야 함").context("생필품 구매도 교통 이용").build(),
                    ExperienceOptionDTO.builder().score(2).text("편의점까지 아파트 단지 한 바퀴").context("기본 시설만 멀리 있음").build(),
                    ExperienceOptionDTO.builder().score(3).text("학교 통학로 정도 거리에 기본 시설").context("필요한 것들이 적당히 있음").build(),
                    ExperienceOptionDTO.builder().score(4).text("산책하면서 갈 수 있을 정도로 가까움").context("다양한 시설이 근처에").build(),
                    ExperienceOptionDTO.builder().score(5).text("건물 1층에 다 있음").context("아파트 상가처럼 도보권 완벽").build()
            );
        }
        if (categoryId == 9 && questionOrder == 2) { // 대중교통
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("지하철역까지 마라톤 1구간").context("1km 이상 멀어서 힘듦").build(),
                    ExperienceOptionDTO.builder().score(2).text("역까지 동네 한 바퀴 돌 정도").context("500m 정도로 부담스러움").build(),
                    ExperienceOptionDTO.builder().score(3).text("학교까지 가는 거리 정도").context("300-500m로 적당함").build(),
                    ExperienceOptionDTO.builder().score(4).text("편의점 가는 거리로 가까움").context("100-300m로 쉽게 이용").build(),
                    ExperienceOptionDTO.builder().score(5).text("집 앞 바로 정류장이나 역").context("100m 미만으로 바로 앞").build()
            );
        }

        // === 10. 인터넷 카테고리 ===
        if (categoryId == 10 && questionOrder == 1) { // 인터넷 속도
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("유튜브 영상도 버퍼링 걸림").context("웹페이지 로딩도 답답함").build(),
                    ExperienceOptionDTO.builder().score(2).text("웹서핑은 되지만 영상 끊김").context("3G폰처럼 기본 사용만 가능").build(),
                    ExperienceOptionDTO.builder().score(3).text("일반 사용에는 문제없음").context("4G폰처럼 HD 영상 무난").build(),
                    ExperienceOptionDTO.builder().score(4).text("4K 영상도 끊김 없음").context("5G폰처럼 고화질도 원활").build(),
                    ExperienceOptionDTO.builder().score(5).text("뭘 해도 순식간").context("초고속으로 모든 용도 완벽").build()
            );
        }
        if (categoryId == 10 && questionOrder == 2) { // WiFi 안정성
            return Arrays.asList(
                    ExperienceOptionDTO.builder().score(1).text("하루에 10번 이상 끊어짐").context("옛날 삐삐처럼 자주 끊김").build(),
                    ExperienceOptionDTO.builder().score(2).text("하루에 3-5번 끊어짐").context("라디오처럼 가끔 지직거림").build(),
                    ExperienceOptionDTO.builder().score(3).text("하루에 1-2번 끊어짐").context("TV처럼 가끔 신호 불량").build(),
                    ExperienceOptionDTO.builder().score(4).text("일주일에 1-2번 끊어짐").context("스마트폰처럼 거의 안 끊김").build(),
                    ExperienceOptionDTO.builder().score(5).text("거의 끊어지지 않음").context("유선랜처럼 항상 안정").build()
            );
        }

        // 기본 옵션 (위에 해당하지 않는 경우)
        return createBasicExperienceOptions();
    }

    private List<ExperienceOptionDTO> createBasicExperienceOptions() {
        return Arrays.asList(
                ExperienceOptionDTO.builder().score(1).text("1점 - 매우 불만족").context("개선이 시급함").build(),
                ExperienceOptionDTO.builder().score(2).text("2점 - 불만족").context("불편함이 많음").build(),
                ExperienceOptionDTO.builder().score(3).text("3점 - 보통").context("무난한 수준").build(),
                ExperienceOptionDTO.builder().score(4).text("4점 - 만족").context("좋은 편").build(),
                ExperienceOptionDTO.builder().score(5).text("5점 - 매우 만족").context("매우 우수함").build()
        );
    }

    private LegalReferenceDTO getLegalReferences(int categoryId) {
        String[][] legalRefs = {
                {"WHO 환경소음 가이드라인 55dB", "소음진동관리법 생활소음 규제"}, // 소음
                {"WHO 급수기준 1.5kgf/cm²", "주택법 시행령 제7조 급수설비"}, // 수압
                {"KS A 3011 조도기준 300럭스", "건축법 시행령 제61조 채광기준"}, // 채광
                {"주차장법 제19조 확보율 90%", "서울시 주차장 설치조례"}, // 주차
                {"건축법 시행령 실내온도 18°C", "WHO 실내온도 권고 18-21°C"}, // 난방
                {"건축법 시행령 환기설비", "ASHRAE 62.1 환기기준"}, // 환기
                {"건축법 제49조 피난시설", "화재예방법 안전관리"}, // 보안
                {"주택임대차보호법 수선의무", "공동주택관리법 유지관리"}, // 관리
                {"국토계획법 생활권 시설배치", "사회복지사업법 접근성"}, // 편의시설
                {"전기통신사업법 보편적서비스", "방송통신발전법 품질기준"} // 인터넷
        };

        if (categoryId >= 1 && categoryId <= 10) {
            return LegalReferenceDTO.builder()
                    .primary(legalRefs[categoryId-1][0])
                    .secondary(legalRefs[categoryId-1][1])
                    .build();
        }
        return null;
    }
}