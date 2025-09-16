package org.example.seasontonebackend.report.service;

import org.example.seasontonebackend.diagnosis.domain.entity.DiagnosisResponse;
import org.example.seasontonebackend.diagnosis.domain.repository.DiagnosisResponseRepository;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.example.seasontonebackend.report.domain.Report;
import org.example.seasontonebackend.report.dto.ReportRequestDto;
import org.example.seasontonebackend.report.dto.ReportResponseDto;
import org.example.seasontonebackend.report.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final DiagnosisResponseRepository diagnosisResponseRepository;

    public ReportService(ReportRepository reportRepository, MemberRepository memberRepository, DiagnosisResponseRepository diagnosisResponseRepository) {
        this.reportRepository = reportRepository;
        this.memberRepository = memberRepository;
        this.diagnosisResponseRepository = diagnosisResponseRepository;
    }

    public String createReport(ReportRequestDto reportRequestDto, Member member) {
        Report report = Report.builder()
                .member(member)
                .userInput(null) // 협상 요구사항 제거
                .build();

        reportRepository.save(report);

        return report.getPublicId();
    }

    public ReportResponseDto getReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NullPointerException("존재하지 않는 리포트입니다!"));

        Member member = report.getMember();
        return buildReportResponse(report, member);
    }

    public ReportResponseDto getReportByPublicId(String publicId) {
        Report report = reportRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NullPointerException("존재하지 않는 리포트입니다!"));

        Member member = report.getMember();
        return buildReportResponse(report, member);
    }

    private ReportResponseDto buildReportResponse(Report report, Member member) {
        List<Member> neighborhoodMembers = memberRepository.findByDong(member.getDong());
        List<Long> neighborhoodMemberIds = neighborhoodMembers.stream().map(Member::getId).collect(Collectors.toList());
        List<DiagnosisResponse> neighborhoodResponses = diagnosisResponseRepository.findByUserIdIn(neighborhoodMemberIds);

        ReportResponseDto.SubjectiveMetricsDto subjectiveMetrics = buildSubjectiveMetrics(member, neighborhoodMembers, neighborhoodResponses);

        List<ReportResponseDto.NegotiationCardDto> negotiationCards = buildNegotiationCards(subjectiveMetrics, null);

        String dong = member.getDong() != null ? member.getDong().trim() : "";
        String building = member.getBuilding() != null ? member.getBuilding().trim() : "";
        String fullAddress = (dong + " " + building).trim();
        
        // 한글 인코딩 문제 해결을 위한 안전한 처리
        if (fullAddress.isEmpty() || fullAddress.equals(" ")) {
            fullAddress = "주소 정보 없음";
        }
        
        System.out.println("DEBUG - Member info: dong=" + dong + ", building=" + building + ", fullAddress=" + fullAddress);
        String conditions = String.format("보증금 %s / 월세 %s / 관리비 %s",
                member.getSecurity() != null ? member.getSecurity().toString() : "미입력",
                member.getRent() != null ? member.getRent().toString() : "미입력",
                member.getMaintenanceFee() != null ? member.getMaintenanceFee().toString() : "미입력");

        long averageResponseAgeDays = calculateAverageResponseAge(neighborhoodResponses);
        String dataRecency = String.format("평균 응답 %d일 전", averageResponseAgeDays);

        int reliabilityScore = calculateReliabilityScore(
                neighborhoodMembers.size(), 
                averageResponseAgeDays, 
                (member.getGpsVerified() != null && member.getGpsVerified()), 
                (member.getContractVerified() != null && member.getContractVerified())
        );

        ReportResponseDto.ReportHeaderDto header = ReportResponseDto.ReportHeaderDto.builder()
                .title(fullAddress + " 임대차 협상 리포트")
                .generatedDate(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .dataPeriod("본 리포트는 최근 1개월 내 참여자 데이터와 공공 데이터를 기반으로 생성되었습니다.")
                .participantCount(neighborhoodMembers.size())
                .dataRecency(dataRecency)
                .reliabilityScore(reliabilityScore)
                .build();

        ReportResponseDto.ContractSummaryDto contractSummary = ReportResponseDto.ContractSummaryDto.builder()
                .address(fullAddress)
                .buildingType(member.getBuildingType())
                .contractType(member.getContractType())
                .conditions(conditions)
                .gpsVerified(member.getGpsVerified() != null && member.getGpsVerified())
                .contractVerified(member.getContractVerified() != null && member.getContractVerified())
                .build();

        return ReportResponseDto.builder()
                .header(header)
                .contractSummary(contractSummary)
                .subjectiveMetrics(subjectiveMetrics)
                .negotiationCards(negotiationCards)
                .policyInfos(buildStaticPolicyInfos())
                .disputeGuide(buildStaticDisputeGuide())
                .build();
    }

    public ReportResponseDto getComprehensiveReport(Member member) {
        List<Member> neighborhoodMembers = memberRepository.findByDong(member.getDong());
        List<Long> neighborhoodMemberIds = neighborhoodMembers.stream().map(Member::getId).collect(Collectors.toList());
        List<DiagnosisResponse> neighborhoodResponses = diagnosisResponseRepository.findByUserIdIn(neighborhoodMemberIds);

        ReportResponseDto.SubjectiveMetricsDto subjectiveMetrics = buildSubjectiveMetrics(member, neighborhoodMembers, neighborhoodResponses);

        List<ReportResponseDto.NegotiationCardDto> negotiationCards = buildNegotiationCards(subjectiveMetrics, null);

        String fullAddress = (member.getDong() != null ? member.getDong() : "") + " " + (member.getBuilding() != null ? member.getBuilding() : "");
        String conditions = String.format("보증금 %s / 월세 %s / 관리비 %s",
                member.getSecurity() != null ? member.getSecurity().toString() : "미입력",
                member.getRent() != null ? member.getRent().toString() : "미입력",
                member.getMaintenanceFee() != null ? member.getMaintenanceFee().toString() : "미입력");

        long averageResponseAgeDays = calculateAverageResponseAge(neighborhoodResponses);
        String dataRecency = String.format("평균 응답 %d일 전", averageResponseAgeDays);

        int reliabilityScore = calculateReliabilityScore(
                Math.max(neighborhoodMembers.size(), 1), // 최소 1명 보장
                averageResponseAgeDays, 
                (member.getGpsVerified() != null && member.getGpsVerified()), 
                (member.getContractVerified() != null && member.getContractVerified())
        );

        ReportResponseDto.ReportHeaderDto header = ReportResponseDto.ReportHeaderDto.builder()
                .title(fullAddress + " 임대차 협상 리포트")
                .generatedDate(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .dataPeriod("본 리포트는 최근 1개월 내 참여자 데이터와 공공 데이터를 기반으로 생성되었습니다.")
                .participantCount(Math.max(neighborhoodMembers.size(), 1)) // 최소 1명 보장
                .dataRecency(dataRecency)
                .reliabilityScore(reliabilityScore)
                .build();

        ReportResponseDto.ContractSummaryDto contractSummary = ReportResponseDto.ContractSummaryDto.builder()
                .address(fullAddress)
                .buildingType(member.getBuildingType())
                .contractType(member.getContractType())
                .conditions(conditions)
                .gpsVerified(member.getGpsVerified() != null && member.getGpsVerified())
                .contractVerified(member.getContractVerified() != null && member.getContractVerified())
                .build();

        return ReportResponseDto.builder()
                .header(header)
                .contractSummary(contractSummary)
                .subjectiveMetrics(subjectiveMetrics)
                .negotiationCards(negotiationCards)
                .policyInfos(buildStaticPolicyInfos()) // 데이터 추가
                .disputeGuide(buildStaticDisputeGuide()) // 데이터 추가
                .build();
    }

    private List<ReportResponseDto.NegotiationCardDto> buildNegotiationCards(ReportResponseDto.SubjectiveMetricsDto subjectiveMetrics, String userInput) {
        List<ReportResponseDto.NegotiationCardDto> cards = new ArrayList<>();

        List<ReportResponseDto.ScoreComparison> sortedScores = subjectiveMetrics.getCategoryScores().stream()
                .filter(s -> s.getMyScore() < s.getNeighborhoodAverage()) 
                .sorted(Comparator.comparingDouble(s -> s.getMyScore() - s.getNeighborhoodAverage()))
                .collect(Collectors.toList());

        int priority = 1;
        for (ReportResponseDto.ScoreComparison score : sortedScores.stream().limit(2).collect(Collectors.toList())) {
            String title = score.getCategory() + " 문제 개선 요구";
            String script = String.format("우리 집의 '%s' 만족도 점수(%.1f점)는 동네 평균(%.1f점)보다 낮습니다. 이 데이터를 근거로 개선을 요구하거나 월세 조정을 제안해볼 수 있습니다.",
                    score.getCategory(), score.getMyScore(), score.getNeighborhoodAverage());

            cards.add(ReportResponseDto.NegotiationCardDto.builder()
                    .priority(priority++)
                    .title(title)
                    .recommendationScript(script)
                    .build());
        }

        // 협상 요구사항 관련 코드 제거
        // if (userInput != null && !userInput.isEmpty()) {
        //     cards.add(ReportResponseDto.NegotiationCardDto.builder()
        //             .priority(priority)
        //             .title("사용자 직접 입력 내용")
        //             .recommendationScript(String.format("추가로, '%s' 문제에 대해 논의가 필요합니다.", userInput))
        //             .build());
        // }

        return cards;
    }

    private ReportResponseDto.SubjectiveMetricsDto buildSubjectiveMetrics(Member currentUser, List<Member> neighborhoodMembers, List<DiagnosisResponse> neighborhoodResponses) {
        List<Member> buildingMembers = neighborhoodMembers.stream()
                .filter(m -> m.getBuilding() != null && m.getBuilding().equals(currentUser.getBuilding()))
                .collect(Collectors.toList());
        List<Long> buildingMemberIds = buildingMembers.stream().map(Member::getId).collect(Collectors.toList());
        List<DiagnosisResponse> buildingResponses = neighborhoodResponses.stream()
                .filter(r -> buildingMemberIds.contains(r.getUserId()))
                .collect(Collectors.toList());

        List<DiagnosisResponse> myResponses = neighborhoodResponses.stream()
                .filter(r -> r.getUserId().equals(currentUser.getId()))
                .collect(Collectors.toList());

        Map<Long, Double> myCategoryAverages = calculateCategoryAverages(myResponses, false);
        Map<Long, Double> buildingCategoryAverages = calculateCategoryAverages(buildingResponses, true);
        Map<Long, Double> neighborhoodCategoryAverages = calculateCategoryAverages(neighborhoodResponses, true);

        List<ReportResponseDto.ScoreComparison> categoryScores = IntStream.rangeClosed(1, 10)
                .mapToObj(categoryId -> ReportResponseDto.ScoreComparison.builder()
                        .category(getCategoryName(categoryId))
                        .myScore(myCategoryAverages.getOrDefault((long)categoryId, 0.0))
                        .buildingAverage(buildingCategoryAverages.getOrDefault((long)categoryId, 0.0))
                        .neighborhoodAverage(neighborhoodCategoryAverages.getOrDefault((long)categoryId, 0.0))
                        .build())
                .collect(Collectors.toList());

        ReportResponseDto.ScoreComparison overallScore = ReportResponseDto.ScoreComparison.builder()
                .category("종합")
                .myScore(myCategoryAverages.values().stream().mapToDouble(d -> d).average().orElse(0.0))
                .buildingAverage(buildingCategoryAverages.values().stream().mapToDouble(d -> d).average().orElse(0.0))
                .neighborhoodAverage(neighborhoodCategoryAverages.values().stream().mapToDouble(d -> d).average().orElse(0.0))
                .build();

        return ReportResponseDto.SubjectiveMetricsDto.builder()
                .overallScore(overallScore)
                .categoryScores(categoryScores)
                .build();
    }

    private Map<Long, Double> calculateCategoryAverages(List<DiagnosisResponse> responses, boolean trimOutliers) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<Integer>> scoresByCategory = responses.stream()
                .collect(Collectors.groupingBy(
                        response -> getCategoryId(response.getQuestionId()),
                        Collectors.mapping(response -> response.getScore().getIntValue(), Collectors.toList())
                ));

        Map<Long, Double> finalAverages = new HashMap<>();
        for (Map.Entry<Long, List<Integer>> entry : scoresByCategory.entrySet()) {
            double average = trimOutliers ? calculateTrimmedMean(entry.getValue()) : entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            finalAverages.put(entry.getKey(), average);
        }

        return finalAverages;
    }

    private double calculateTrimmedMean(List<Integer> scores) {
        if (scores.size() < 5) {
            return scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        }

        List<Integer> sortedScores = new ArrayList<>(scores);
        Collections.sort(sortedScores);

        int trimSize = (int) Math.floor(sortedScores.size() * 0.1);

        List<Integer> trimmedList = sortedScores.subList(trimSize, sortedScores.size() - trimSize);

        return trimmedList.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private long getCategoryId(long questionId) {
        return (questionId - 1) / 2 + 1;
    }

    private String getCategoryName(long categoryId) {
        switch ((int)categoryId) {
            case 1: return "소음";
            case 2: return "수압/온수";
            case 3: return "채광";
            case 4: return "주차/교통";
            case 5: return "난방";
            case 6: return "환기/습도";
            case 7: return "보안/안전";
            case 8: return "건물 관리";
            case 9: return "편의시설";
            case 10: return "인터넷/통신";
            default: return "기타";
        }
    }

    private long calculateAverageResponseAge(List<DiagnosisResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return 0L;
        }

        LocalDateTime now = LocalDateTime.now();
        long totalDays = 0;
        for (DiagnosisResponse response : responses) {
            totalDays += ChronoUnit.DAYS.between(response.getCreatedAt(), now);
        }
        return totalDays / responses.size();
    }

    private int calculateReliabilityScore(int participantCount, long averageResponseAgeDays, boolean isGpsVerified, boolean isContractVerified) {
        int score = 50;
        score += Math.min(participantCount * 2, 30);
        if (averageResponseAgeDays <= 7) score += 10;
        else if (averageResponseAgeDays <= 14) score += 5;
        else if (averageResponseAgeDays > 30 && averageResponseAgeDays <= 60) score -= 5;
        else if (averageResponseAgeDays > 60) score -= 10;
        if (isGpsVerified) score += 10;
        if (isContractVerified) score += 10;
        return Math.max(0, Math.min(100, score));
    }

    // --- 정적 데이터 생성을 위한 헬퍼 메소드 ---

    private List<ReportResponseDto.PolicyInfoDto> buildStaticPolicyInfos() {
        return Arrays.asList(
                ReportResponseDto.PolicyInfoDto.builder()
                        .title("청년 월세 특별지원")
                        .description("국토부에서 제공하는 청년층 월세 지원 정책")
                        .link("https://www.molit.go.kr")
                        .build(),
                ReportResponseDto.PolicyInfoDto.builder()
                        .title("서울시 청년 월세 지원금")
                        .description("서울 거주 청년을 위한 월세 지원금")
                        .link("https://youth.seoul.go.kr")
                        .build(),
                ReportResponseDto.PolicyInfoDto.builder()
                        .title("전세보증금 반환보증 (HUG)")
                        .description("전세보증금 반환을 보장하는 제도")
                        .link("https://www.hug.or.kr")
                        .build()
        );
    }

    private ReportResponseDto.DisputeGuideDto buildStaticDisputeGuide() {
        return ReportResponseDto.DisputeGuideDto.builder()
                .relatedLaw("주택임대차보호법 제6조의2 (임대인의 수선유지 의무)")
                .committeeInfo("서울서부 임대차분쟁조정위원회 (연락처: 02-123-4567)")
                .formDownloadLink("#") // Placeholder link
                .build();
    }
}