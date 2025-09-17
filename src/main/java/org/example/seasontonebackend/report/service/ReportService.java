package org.example.seasontonebackend.report.service;

import org.example.seasontonebackend.diagnosis.domain.entity.DiagnosisResponse;
import org.example.seasontonebackend.diagnosis.domain.repository.DiagnosisResponseRepository;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.example.seasontonebackend.report.domain.Report;
import org.example.seasontonebackend.report.dto.ReportRequestDto;
import org.example.seasontonebackend.report.dto.ReportResponseDto;
import org.example.seasontonebackend.report.repository.ReportRepository;
import org.example.seasontonebackend.smartdiagnosis.application.SmartDiagnosisService;
import org.example.seasontonebackend.smartdiagnosis.dto.SmartDiagnosisResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final DiagnosisResponseRepository diagnosisResponseRepository;
    private final SmartDiagnosisService smartDiagnosisService;
    
    // 동시 리포트 생성을 위한 스레드 풀 (최대 10개 동시 처리)
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    // JSON 변환을 위한 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportService(ReportRepository reportRepository, MemberRepository memberRepository, DiagnosisResponseRepository diagnosisResponseRepository, SmartDiagnosisService smartDiagnosisService) {
        this.reportRepository = reportRepository;
        this.memberRepository = memberRepository;
        this.diagnosisResponseRepository = diagnosisResponseRepository;
        this.smartDiagnosisService = smartDiagnosisService;
    }

    @Transactional
    public String createReport(ReportRequestDto reportRequestDto, Member member) {
        // 동시 생성 시 안전성을 위한 트랜잭션 처리
        Report report = Report.builder()
                .member(member)
                .userInput(reportRequestDto.getReportContent()) // 협상 요구사항 저장
                .reportType(reportRequestDto.getReportType()) // 리포트 타입 저장
                .isShareable(true) // 공유 가능으로 설정
                .build();

        reportRepository.save(report);

        // 공유용 리포트 데이터 생성 및 저장
        try {
            ReportResponseDto sharedReportData = buildReportResponse(report, member);
            String jsonData = objectMapper.writeValueAsString(sharedReportData);
            report.setSharedReportData(jsonData);
            reportRepository.save(report);
        } catch (Exception e) {
            // JSON 변환 실패 시에도 리포트 생성은 계속 진행
            System.err.println("공유용 데이터 생성 실패: " + e.getMessage());
        }

        return report.getPublicId();
    }
    
    // 비동기 리포트 생성 (대용량 처리용)
    public CompletableFuture<String> createReportAsync(ReportRequestDto reportRequestDto, Member member) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createReport(reportRequestDto, member);
            } catch (Exception e) {
                throw new RuntimeException("리포트 생성 실패: " + e.getMessage(), e);
            }
        }, executorService);
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

        // 공유 가능한지 확인
        if (!Boolean.TRUE.equals(report.getIsShareable())) {
            throw new RuntimeException("공유할 수 없는 리포트입니다.");
        }

        // 저장된 공유용 데이터가 있으면 반환
        if (report.getSharedReportData() != null && !report.getSharedReportData().isEmpty()) {
            try {
                return objectMapper.readValue(report.getSharedReportData(), ReportResponseDto.class);
            } catch (Exception e) {
                System.err.println("저장된 공유 데이터 파싱 실패: " + e.getMessage());
                // 파싱 실패 시 실시간 생성으로 폴백
            }
        }

        // 저장된 데이터가 없으면 실시간 생성 (기존 로직)
        Member member = report.getMember();
        return buildReportResponse(report, member);
    }

    private ReportResponseDto buildReportResponse(Report report, Member member) {
        List<Member> neighborhoodMembers = memberRepository.findByDong(member.getDong());
        List<Long> neighborhoodMemberIds = neighborhoodMembers.stream().map(Member::getId).collect(Collectors.toList());
        List<DiagnosisResponse> neighborhoodResponses = diagnosisResponseRepository.findByUserIdIn(neighborhoodMemberIds);

        ReportResponseDto.SubjectiveMetricsDto subjectiveMetrics = buildSubjectiveMetrics(member, neighborhoodMembers, neighborhoodResponses);

        List<ReportResponseDto.NegotiationCardDto> negotiationCards = buildNegotiationCards(subjectiveMetrics, report.getUserInput(), report.getReportType());

        // 스마트 진단 데이터 가져오기
        SmartDiagnosisResponseDTO.SmartDiagnosisSummary smartDiagnosisData = null;
        try {
            smartDiagnosisData = smartDiagnosisService.getSmartDiagnosisSummary(member);
        } catch (Exception e) {
            System.err.println("스마트 진단 데이터 조회 실패: " + e.getMessage());
        }

        String dong = member.getDong() != null ? member.getDong().trim() : "";
        String building = member.getBuilding() != null ? member.getBuilding().trim() : "";
        
        // 한글 인코딩 문제 해결을 위한 안전한 처리
        String fullAddress;
        if (dong.isEmpty() && building.isEmpty()) {
            fullAddress = "주소 정보 없음";
        } else if (dong.isEmpty()) {
            fullAddress = building;
        } else if (building.isEmpty()) {
            fullAddress = dong;
        } else {
            fullAddress = dong + " " + building;
        }
        
        // 한글이 깨진 경우 감지 및 처리
        if (fullAddress.contains("?") || fullAddress.contains("�")) {
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
                .reportType(report.getReportType() != null ? report.getReportType() : "free")
                .header(header)
                .contractSummary(contractSummary)
                .subjectiveMetrics(subjectiveMetrics)
                .negotiationCards(negotiationCards)
                .policyInfos(buildPolicyInfos(report.getReportType()))
                .disputeGuide(buildDisputeGuide(report.getReportType()))
                .smartDiagnosisData(smartDiagnosisData)
                .build();
    }

    public ReportResponseDto getComprehensiveReport(Member member) {
        List<Member> neighborhoodMembers = memberRepository.findByDong(member.getDong());
        List<Long> neighborhoodMemberIds = neighborhoodMembers.stream().map(Member::getId).collect(Collectors.toList());
        List<DiagnosisResponse> neighborhoodResponses = diagnosisResponseRepository.findByUserIdIn(neighborhoodMemberIds);

        ReportResponseDto.SubjectiveMetricsDto subjectiveMetrics = buildSubjectiveMetrics(member, neighborhoodMembers, neighborhoodResponses);

        List<ReportResponseDto.NegotiationCardDto> negotiationCards = buildNegotiationCards(subjectiveMetrics, null, "free");

        // 스마트 진단 데이터 가져오기
        SmartDiagnosisResponseDTO.SmartDiagnosisSummary smartDiagnosisData = null;
        try {
            smartDiagnosisData = smartDiagnosisService.getSmartDiagnosisSummary(member);
        } catch (Exception e) {
            System.err.println("스마트 진단 데이터 조회 실패: " + e.getMessage());
        }

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
                .policyInfos(buildPolicyInfos("free"))
                .disputeGuide(buildDisputeGuide("free"))
                .smartDiagnosisData(smartDiagnosisData)
                .build();
    }

    private List<ReportResponseDto.NegotiationCardDto> buildNegotiationCards(ReportResponseDto.SubjectiveMetricsDto subjectiveMetrics, String userInput, String reportType) {
        List<ReportResponseDto.NegotiationCardDto> cards = new ArrayList<>();
        boolean isPremium = "premium".equals(reportType);

        List<ReportResponseDto.ScoreComparison> sortedScores = subjectiveMetrics.getCategoryScores().stream()
                .filter(s -> s.getMyScore() < s.getNeighborhoodAverage()) 
                .sorted(Comparator.comparingDouble(s -> s.getMyScore() - s.getNeighborhoodAverage()))
                .collect(Collectors.toList());

        // 협상 카드가 없을 경우 기본 카드 생성
        if (sortedScores.isEmpty()) {
            sortedScores = subjectiveMetrics.getCategoryScores().stream()
                    .sorted(Comparator.comparingDouble(s -> s.getMyScore() - s.getNeighborhoodAverage()))
                    .limit(2)
                    .collect(Collectors.toList());
        }

        int priority = 1;
        for (ReportResponseDto.ScoreComparison score : sortedScores.stream().limit(2).collect(Collectors.toList())) {
            String title = score.getCategory() + " 문제 개선 요구";
            String script = String.format("우리 집의 '%s' 만족도 점수(%.1f점)는 동네 평균(%.1f점)보다 낮습니다. 이 데이터를 근거로 개선을 요구하거나 월세 조정을 제안해볼 수 있습니다.",
                    score.getCategory(), score.getMyScore(), score.getNeighborhoodAverage());

            ReportResponseDto.NegotiationCardDto.NegotiationCardDtoBuilder cardBuilder = ReportResponseDto.NegotiationCardDto.builder()
                    .priority(priority++)
                    .title(title)
                    .recommendationScript(script);

            // 프리미엄 리포트인 경우 추가 필드들 설정
            if (isPremium) {
                cardBuilder
                    .successProbability(generateSuccessProbability(score))
                    .alternativeStrategy(generateAlternativeStrategy(score))
                    .expertTip(generateExpertTip(score));
            }

            cards.add(cardBuilder.build());
        }

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
                        .myScore(myCategoryAverages.getOrDefault((long)categoryId, 3.0)) // 기본값 3.0
                        .buildingAverage(buildingCategoryAverages.getOrDefault((long)categoryId, 3.5)) // 기본값 3.5
                        .neighborhoodAverage(neighborhoodCategoryAverages.getOrDefault((long)categoryId, 3.5)) // 기본값 3.5
                        .build())
                .collect(Collectors.toList());

        ReportResponseDto.ScoreComparison overallScore = ReportResponseDto.ScoreComparison.builder()
                .category("종합")
                .myScore(myCategoryAverages.values().stream().mapToDouble(d -> d).average().orElse(3.0)) // 기본값 3.0
                .buildingAverage(buildingCategoryAverages.values().stream().mapToDouble(d -> d).average().orElse(3.5)) // 기본값 3.5
                .neighborhoodAverage(neighborhoodCategoryAverages.values().stream().mapToDouble(d -> d).average().orElse(3.5)) // 기본값 3.5
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

    private List<ReportResponseDto.PolicyInfoDto> buildPolicyInfos(String reportType) {
        boolean isPremium = "premium".equals(reportType);
        
        List<ReportResponseDto.PolicyInfoDto> policies = new ArrayList<>();
        
        // 청년 월세 특별지원
        ReportResponseDto.PolicyInfoDto.PolicyInfoDtoBuilder policy1 = ReportResponseDto.PolicyInfoDto.builder()
                .title("청년 월세 특별지원")
                .description("국토부에서 제공하는 청년층 월세 지원 정책")
                .link("https://www.molit.go.kr");
        
        if (isPremium) {
            policy1.isEligible(true)
                   .applicationDeadline("2025.12.31")
                   .requiredDocuments(Arrays.asList("신분증", "소득증명서", "임대차계약서"));
        }
        policies.add(policy1.build());
        
        // 서울시 청년 월세 지원금
        ReportResponseDto.PolicyInfoDto.PolicyInfoDtoBuilder policy2 = ReportResponseDto.PolicyInfoDto.builder()
                .title("서울시 청년 월세 지원금")
                .description("서울 거주 청년을 위한 월세 지원금")
                .link("https://youth.seoul.go.kr");
        
        if (isPremium) {
            policy2.isEligible(true)
                   .applicationDeadline("2025.11.30")
                   .requiredDocuments(Arrays.asList("주민등록등본", "소득증명서", "임대차계약서", "통장사본"));
        }
        policies.add(policy2.build());
        
        // 전세보증금 반환보증 (HUG)
        ReportResponseDto.PolicyInfoDto.PolicyInfoDtoBuilder policy3 = ReportResponseDto.PolicyInfoDto.builder()
                .title("전세보증금 반환보증 (HUG)")
                .description("전세보증금 반환을 보장하는 제도")
                .link("https://www.hug.or.kr");
        
        if (isPremium) {
            policy3.isEligible(false)
                   .applicationDeadline("상시")
                   .requiredDocuments(Arrays.asList("전세계약서", "신분증", "소득증명서"));
        }
        policies.add(policy3.build());
        
        return policies;
    }

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

    private ReportResponseDto.DisputeGuideDto buildDisputeGuide(String reportType) {
        boolean isPremium = "premium".equals(reportType);
        
        ReportResponseDto.DisputeGuideDto.DisputeGuideDtoBuilder builder = ReportResponseDto.DisputeGuideDto.builder()
                .relatedLaw("주택임대차보호법 제6조의2 (임대인의 수선유지 의무)")
                .committeeInfo("서울서부 임대차분쟁조정위원회 (연락처: 02-123-4567)")
                .formDownloadLink("#"); // Placeholder link
        
        if (isPremium) {
            // 분쟁 해결 로드맵
            List<ReportResponseDto.DisputeRoadmapStepDto> roadmap = Arrays.asList(
                ReportResponseDto.DisputeRoadmapStepDto.builder()
                    .step(1)
                    .title("내용증명 발송")
                    .description("임대인에게 수선 요구 내용증명 발송")
                    .estimatedTime("1-2주")
                    .cost("3,000원")
                    .build(),
                ReportResponseDto.DisputeRoadmapStepDto.builder()
                    .step(2)
                    .title("분쟁조정위원회 신청")
                    .description("내용증명 무응답 시 분쟁조정위원회 신청")
                    .estimatedTime("2-4주")
                    .cost("무료")
                    .build(),
                ReportResponseDto.DisputeRoadmapStepDto.builder()
                    .step(3)
                    .title("소송 제기")
                    .description("조정 실패 시 소송 제기 (최후 수단)")
                    .estimatedTime("3-6개월")
                    .cost("소송비용 별도")
                    .build()
            );
            
            // 전문가 상담 정보
            ReportResponseDto.ExpertConsultationDto expertConsultation = ReportResponseDto.ExpertConsultationDto.builder()
                    .available(true)
                    .price(50000)
                    .duration("15분")
                    .contactInfo("02-1234-5678")
                    .build();
            
            builder.disputeRoadmap(roadmap)
                   .expertConsultation(expertConsultation);
        }
        
        return builder.build();
    }

    private ReportResponseDto.DisputeGuideDto buildStaticDisputeGuide() {
        return ReportResponseDto.DisputeGuideDto.builder()
                .relatedLaw("주택임대차보호법 제6조의2 (임대인의 수선유지 의무)")
                .committeeInfo("서울서부 임대차분쟁조정위원회 (연락처: 02-123-4567)")
                .formDownloadLink("#") // Placeholder link
                .build();
    }
    
    // 프리미엄 협상 카드 데이터 생성 메서드들
    private String generateSuccessProbability(ReportResponseDto.ScoreComparison score) {
        double scoreDiff = score.getNeighborhoodAverage() - score.getMyScore();
        int baseProbability = 50;
        
        if (scoreDiff > 1.0) {
            baseProbability = 85; // 큰 차이면 높은 성공률
        } else if (scoreDiff > 0.5) {
            baseProbability = 72; // 중간 차이면 중간 성공률
        } else {
            baseProbability = 45; // 작은 차이면 낮은 성공률
        }
        
        return baseProbability + "%";
    }
    
    private String generateAlternativeStrategy(ReportResponseDto.ScoreComparison score) {
        String category = score.getCategory();
        
        switch (category) {
            case "소음":
                return "소음 문제가 지속되면 주택임대차보호법 제6조의2에 따라 임대인에게 수선 의무가 있습니다. 내용증명으로 요구서를 보내세요.";
            case "수압/온수":
                return "수압 문제는 우리 건물 평균 대비 50% 낮습니다. 수선 의무가 있으니 보일러/배관 점검을 요구하세요.";
            case "채광":
                return "채광 부족은 건물 구조상 개선이 어려우므로, 월세 인상률 동결 또는 관리비 할인을 요구하세요.";
            case "주차/교통":
                return "주차 문제는 동네 인프라와 관련이 있으므로, 임대인과 협의하여 대안을 모색하세요.";
            default:
                return "법적 근거를 바탕으로 단계적 접근을 권장합니다.";
        }
    }
    
    private String generateExpertTip(ReportResponseDto.ScoreComparison score) {
        String category = score.getCategory();
        
        switch (category) {
            case "소음":
                return "소음 측정 데이터와 이전 세입자 증언을 함께 제시하면 성공 확률이 높아집니다.";
            case "수압/온수":
                return "수압 측정 앱으로 객관적 데이터를 수집하여 제시하면 설득력이 높아집니다.";
            case "채광":
                return "채광 측정 앱으로 객관적 데이터를 수집하여 제시하면 설득력이 높아집니다.";
            case "주차/교통":
                return "주차 공간 확보 방안을 구체적으로 제시하면 협상에 유리합니다.";
            default:
                return "객관적 데이터와 함께 제시하면 성공 확률이 높아집니다.";
        }
    }

    /**
     * 공유 가능한 URL 생성
     */
    public String generateShareUrl(String reportId, boolean isPremium) {
        try {
            // 리포트 ID로 리포트 조회
            Report report = reportRepository.findByPublicId(reportId)
                    .orElseThrow(() -> new RuntimeException("리포트를 찾을 수 없습니다: " + reportId));
            
            // 공유 가능한 URL 생성 (프론트엔드 도메인 + 공개 경로)
            String baseUrl = "https://rental-lovat-theta.vercel.app";
            String sharePath = isPremium ? 
                    "/report/" + reportId + "?type=premium" : 
                    "/report/" + reportId;
            
            return baseUrl + sharePath;
        } catch (Exception e) {
            throw new RuntimeException("공유 URL 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}