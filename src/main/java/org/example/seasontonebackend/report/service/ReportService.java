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
import org.example.seasontonebackend.officetel.application.OfficetelService;
import org.example.seasontonebackend.officetel.dto.OfficetelMarketDataResponseDTO;
import org.example.seasontonebackend.villa.application.VillaService;
import org.example.seasontonebackend.villa.dto.VillaMarketDataResponseDTO;
import org.example.seasontonebackend.common.service.AddressService;
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
    private final OfficetelService officetelService;
    private final VillaService villaService;
    private final AddressService addressService;
    
    // 동시 리포트 생성을 위한 스레드 풀 (최대 10개 동시 처리)
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    // JSON 변환을 위한 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportService(ReportRepository reportRepository, MemberRepository memberRepository, DiagnosisResponseRepository diagnosisResponseRepository, SmartDiagnosisService smartDiagnosisService, OfficetelService officetelService, VillaService villaService, AddressService addressService) {
        this.reportRepository = reportRepository;
        this.memberRepository = memberRepository;
        this.diagnosisResponseRepository = diagnosisResponseRepository;
        this.smartDiagnosisService = smartDiagnosisService;
        this.officetelService = officetelService;
        this.villaService = villaService;
        this.addressService = addressService;
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

        // 실거래가 데이터 가져오기
        ReportResponseDto.ObjectiveMetricsDto objectiveMetrics = buildObjectiveMetrics(member);

        ReportResponseDto.ReportResponseDtoBuilder builder = ReportResponseDto.builder()
                .reportType(report.getReportType() != null ? report.getReportType() : "free")
                .header(header)
                .contractSummary(contractSummary)
                .subjectiveMetrics(subjectiveMetrics)
                .objectiveMetrics(objectiveMetrics)
                .negotiationCards(negotiationCards)
                .policyInfos(buildPolicyInfos(report.getReportType()))
                .disputeGuide(buildDisputeGuide(report.getReportType()))
                .smartDiagnosisData(smartDiagnosisData);

        // 프리미엄 리포트인 경우 추가 기능들 추가
        if ("premium".equals(report.getReportType())) {
            builder.premiumFeatures(buildPremiumFeatures(member));
        }

        return builder.build();
    }
    
    /**
     * 프리미엄 기능 데이터 생성
     */
    private ReportResponseDto.PremiumFeaturesDto buildPremiumFeatures(Member member) {
        // 시계열 분석 데이터 생성
        ReportResponseDto.TimeSeriesAnalysisDto timeSeriesAnalysis = buildTimeSeriesAnalysis(member);
        
        // 스마트 진단 데이터 (이미 있으면 그대로 사용)
        Object smartDiagnosis = null;
        try {
            smartDiagnosis = smartDiagnosisService.getSmartDiagnosisSummary(member);
        } catch (Exception e) {
            System.err.println("스마트 진단 데이터 조회 실패: " + e.getMessage());
        }
        
        // 문서 생성 기능
        ReportResponseDto.DocumentGenerationDto documentGeneration = ReportResponseDto.DocumentGenerationDto.builder()
                .available(true)
                .templates(Arrays.asList("수선 요구서", "내용증명", "법적 고지서"))
                .build();
        
        // 전문가 상담 기능
        ReportResponseDto.ExpertConsultationDto expertConsultation = ReportResponseDto.ExpertConsultationDto.builder()
                .available(true)
                .price(50000)
                .duration("15분")
                .contactInfo("02-1234-5678")
                .build();
        
        // 공유 옵션
        ReportResponseDto.SharingOptionsDto sharingOptions = ReportResponseDto.SharingOptionsDto.builder()
                .pdfDownload(true)
                .emailShare(true)
                .socialShare(true)
                .linkShare(true)
                .build();
        
        return ReportResponseDto.PremiumFeaturesDto.builder()
                .timeSeriesAnalysis(timeSeriesAnalysis)
                .smartDiagnosis(smartDiagnosis)
                .documentGeneration(documentGeneration)
                .expertConsultation(expertConsultation)
                .sharingOptions(sharingOptions)
                .build();
    }
    
    /**
     * 시계열 분석 데이터 생성
     */
    private ReportResponseDto.TimeSeriesAnalysisDto buildTimeSeriesAnalysis(Member member) {
        try {
            // 법정동코드 추출
            String lawdCd = addressService.extractLawdCd(member.getDong());
            
            // 건물 유형에 따라 적절한 시계열 데이터 가져오기 (6개월 데이터)
            Map<String, Object> timeSeriesData;
            if (member.getBuildingType() != null && 
                (member.getBuildingType().contains("빌라") || member.getBuildingType().contains("다세대"))) {
                timeSeriesData = villaService.getTimeSeriesAnalysis(lawdCd, 6);
            } else {
                timeSeriesData = officetelService.getTimeSeriesAnalysis(lawdCd, 6);
            }
            
            // 시계열 데이터 변환
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> timeSeries = (List<Map<String, Object>>) timeSeriesData.get("timeSeries");
            @SuppressWarnings("unchecked")
            Map<String, Object> analysis = (Map<String, Object>) timeSeriesData.get("analysis");
            
            // 프론트엔드 형식으로 변환
            List<ReportResponseDto.RentTrendDto> rentTrend = new ArrayList<>();
            if (timeSeries != null) {
                for (Map<String, Object> monthData : timeSeries) {
                    rentTrend.add(ReportResponseDto.RentTrendDto.builder()
                            .month((String) monthData.get("period"))
                            .averageRent(((Number) monthData.get("averageRent")).doubleValue())
                            .build());
                }
            }
            
            // 분석 결과 추출
            double marketVolatility = analysis != null ? 
                ((Number) analysis.getOrDefault("totalChangeRate", 0)).doubleValue() / 100.0 : 0.0;
            int predictionConfidence = 85; // 기본값
            
            return ReportResponseDto.TimeSeriesAnalysisDto.builder()
                    .rentTrend(rentTrend)
                    .marketVolatility(marketVolatility)
                    .predictionConfidence(predictionConfidence)
                    .period("6개월")
                    .dataSource("공공데이터포털 실거래가 API")
                    .build();
                    
        } catch (Exception e) {
            System.err.println("시계열 분석 데이터 생성 실패: " + e.getMessage());
            
            // 기본 시계열 데이터 생성
            return createMockTimeSeriesAnalysis();
        }
    }
    
    /**
     * 목업 시계열 분석 데이터 생성 (6개월, 11.9% 상승률)
     */
    private ReportResponseDto.TimeSeriesAnalysisDto createMockTimeSeriesAnalysis() {
        List<ReportResponseDto.RentTrendDto> rentTrend = Arrays.asList(
            ReportResponseDto.RentTrendDto.builder().month("2025-04").averageRent(590000.0).build(),
            ReportResponseDto.RentTrendDto.builder().month("2025-05").averageRent(610000.0).build(),
            ReportResponseDto.RentTrendDto.builder().month("2025-06").averageRent(630000.0).build(),
            ReportResponseDto.RentTrendDto.builder().month("2025-07").averageRent(650000.0).build(),
            ReportResponseDto.RentTrendDto.builder().month("2025-08").averageRent(660000.0).build(),
            ReportResponseDto.RentTrendDto.builder().month("2025-09").averageRent(660000.0).build()
        );
        
        return ReportResponseDto.TimeSeriesAnalysisDto.builder()
                .rentTrend(rentTrend)
                .marketVolatility(0.119) // 11.9%
                .predictionConfidence(85)
                .period("6개월")
                .dataSource("시뮬레이션 데이터")
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
            String script = String.format("우리 집의 '%s' 만족도 점수(%.1f점)는 동네 평균(%.1f점)보다 낮습니다. 이 객관적 데이터를 바탕으로 합리적인 개선 방안을 함께 모색해보시는 것은 어떨까요?",
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
                .description("국토부에서 제공하는 청년층 월세 지원 정책으로, 월세의 일부를 지원받을 수 있습니다.")
                .link("https://www.bokjiro.go.kr/ssis-tbu/twataa/wlfareInfo/moveTWAT52011M.do?wlfareInfoId=WLF00004661");
        
        if (isPremium) {
            policy1.isEligible(true)
                   .applicationDeadline("2025.12.31")
                   .requiredDocuments(Arrays.asList("신분증", "소득증명서", "임대차계약서"));
        }
        policies.add(policy1.build());
        
        // 서울시 청년 월세 지원금
        ReportResponseDto.PolicyInfoDto.PolicyInfoDtoBuilder policy2 = ReportResponseDto.PolicyInfoDto.builder()
                .title("서울시 청년 월세 지원금")
                .description("서울 거주 청년을 위한 월세 지원금으로, 거주 지역과 소득에 따라 차등 지원됩니다.")
                .link("https://housing.seoul.go.kr/site/main/content/sh01_060513");
        
        if (isPremium) {
            policy2.isEligible(true)
                   .applicationDeadline("2025.11.30")
                   .requiredDocuments(Arrays.asList("주민등록등본", "소득증명서", "임대차계약서", "통장사본"));
        }
        policies.add(policy2.build());
        
        // 전세보증금 반환보증 (HUG)
        ReportResponseDto.PolicyInfoDto.PolicyInfoDtoBuilder policy3 = ReportResponseDto.PolicyInfoDto.builder()
                .title("전세보증금 반환보증 (HUG)")
                .description("전세보증금 반환을 보장하는 제도로, 전세 사기 피해를 예방할 수 있습니다.")
                .link("https://www.khug.or.kr/hug/web/ig/dr/igdr000001.jsp");
        
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
                        .description("국토부에서 제공하는 청년층 월세 지원 정책으로, 월세의 일부를 지원받을 수 있습니다.")
                        .link("https://www.bokjiro.go.kr/ssis-tbu/twataa/wlfareInfo/moveTWAT52011M.do?wlfareInfoId=WLF00004661")
                        .build(),
                ReportResponseDto.PolicyInfoDto.builder()
                        .title("서울시 청년 월세 지원금")
                        .description("서울 거주 청년을 위한 월세 지원금으로, 거주 지역과 소득에 따라 차등 지원됩니다.")
                        .link("https://housing.seoul.go.kr/site/main/content/sh01_060513")
                        .build(),
                ReportResponseDto.PolicyInfoDto.builder()
                        .title("전세보증금 반환보증 (HUG)")
                        .description("전세보증금 반환을 보장하는 제도로, 전세 사기 피해를 예방할 수 있습니다.")
                        .link("https://www.khug.or.kr/hug/web/ig/dr/igdr000001.jsp")
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
                return "소음 문제가 지속될 경우 주택임대차보호법 제6조의2에 따라 임대인에게 수선 의무가 있습니다. 정중하게 개선 방안을 논의해보세요.";
            case "수압/온수":
                return "수압 문제는 우리 건물 평균 대비 낮은 수치를 보입니다. 보일러/배관 점검을 함께 검토해보시는 것은 어떨까요?";
            case "채광":
                return "채광 부족은 건물 구조상 개선이 어려운 부분입니다. 월세 인상률 동결이나 관리비 할인 등 대안을 함께 고려해보세요.";
            case "주차/교통":
                return "주차 문제는 동네 인프라와 관련이 있습니다. 임대인과 협의하여 실현 가능한 대안을 모색해보세요.";
            default:
                return "객관적 데이터를 바탕으로 상호 존중하는 자세로 단계적 접근을 권장합니다.";
        }
    }
    
    private String generateExpertTip(ReportResponseDto.ScoreComparison score) {
        String category = score.getCategory();
        
        switch (category) {
            case "소음":
                return "소음 측정 데이터와 이전 세입자 증언을 함께 제시하면 협상에 도움이 됩니다.";
            case "수압/온수":
                return "수압 측정 앱으로 객관적 데이터를 수집하여 제시하면 상호 이해에 도움이 됩니다.";
            case "채광":
                return "채광 측정 앱으로 객관적 데이터를 수집하여 제시하면 합리적 논의에 도움이 됩니다.";
            case "주차/교통":
                return "주차 공간 확보 방안을 구체적으로 제시하면 실현 가능한 해결책을 찾는데 도움이 됩니다.";
            default:
                return "객관적 데이터를 바탕으로 상호 존중하는 자세로 접근하면 좋은 결과를 얻을 수 있습니다.";
        }
    }

    /**
     * 실거래가 데이터를 기반으로 객관적 지표 생성
     */
    private ReportResponseDto.ObjectiveMetricsDto buildObjectiveMetrics(Member member) {
        try {
            // 실제 주소에서 법정동코드 추출
            String lawdCd = addressService.extractLawdCd(member.getDong());
            
            if (lawdCd == null || lawdCd.isEmpty()) {
                return createMockObjectiveMetrics(member);
            }
            
            // 건물 유형에 따라 적절한 API 호출
            String buildingType = member.getBuildingType();
            if (buildingType != null && (buildingType.contains("빌라") || buildingType.contains("다세대"))) {
                // 빌라 API 호출
                List<VillaMarketDataResponseDTO> jeonseData = villaService.getJeonseMarketData(lawdCd);
                List<VillaMarketDataResponseDTO> monthlyRentData = villaService.getMonthlyRentMarketData(lawdCd);
                
                // 빌라 데이터를 오피스텔 형식으로 변환
                List<OfficetelMarketDataResponseDTO> convertedJeonseData = convertVillaToOfficetelData(jeonseData);
                List<OfficetelMarketDataResponseDTO> convertedMonthlyRentData = convertVillaToOfficetelData(monthlyRentData);
                
                return analyzeMarketData(convertedJeonseData, convertedMonthlyRentData, member);
            } else {
                // 오피스텔 API 호출 (기본값)
                List<OfficetelMarketDataResponseDTO> jeonseData = officetelService.getJeonseMarketData(lawdCd);
                List<OfficetelMarketDataResponseDTO> monthlyRentData = officetelService.getMonthlyRentMarketData(lawdCd);
                
                return analyzeMarketData(jeonseData, monthlyRentData, member);
            }
            
        } catch (Exception e) {
            System.err.println("실거래가 데이터 조회 실패: " + e.getMessage());
            return createMockObjectiveMetrics(member);
        }
    }
    
    
    /**
     * 빌라 데이터를 오피스텔 형식으로 변환
     */
    private List<OfficetelMarketDataResponseDTO> convertVillaToOfficetelData(List<VillaMarketDataResponseDTO> villaData) {
        return villaData.stream()
                .map(villa -> OfficetelMarketDataResponseDTO.builder()
                        .neighborhood(villa.getNeighborhood())
                        .avgDeposit(villa.getAvgDeposit())
                        .avgMonthlyRent(villa.getAvgMonthlyRent())
                        .transactionCount(villa.getTransactionCount())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 실제 시장 데이터 분석
     */
    private ReportResponseDto.ObjectiveMetricsDto analyzeMarketData(
            List<OfficetelMarketDataResponseDTO> jeonseData,
            List<OfficetelMarketDataResponseDTO> monthlyRentData,
            Member member) {
        
        // 전세 데이터 분석
        double avgJeonseDeposit = jeonseData.stream()
                .mapToDouble(OfficetelMarketDataResponseDTO::getAvgDeposit)
                .average()
                .orElse(0.0);
        
        // 월세 데이터 분석
        double avgMonthlyRent = monthlyRentData.stream()
                .mapToDouble(OfficetelMarketDataResponseDTO::getAvgMonthlyRent)
                .average()
                .orElse(0.0);
        
        double avgDeposit = monthlyRentData.stream()
                .mapToDouble(OfficetelMarketDataResponseDTO::getAvgDeposit)
                .average()
                .orElse(0.0);
        
        // 사용자 계약 조건과 비교
        Long userDeposit = member.getSecurity();
        Integer userRent = member.getRent();
        
        // 시세 대비 분석
        String marketAnalysis = generateMarketAnalysis(userDeposit, userRent, avgDeposit, avgMonthlyRent);
        
        // 주변 동네 비교 데이터
        List<ReportResponseDto.NeighborhoodComparisonDto> neighborhoodComparisons = jeonseData.stream()
                .limit(5) // 상위 5개 동네
                .map(data -> ReportResponseDto.NeighborhoodComparisonDto.builder()
                        .neighborhoodName(data.getNeighborhood())
                        .averageDeposit(data.getAvgDeposit())
                        .averageMonthlyRent(data.getAvgMonthlyRent())
                        .transactionCount(data.getTransactionCount())
                        .build())
                .collect(Collectors.toList());
        
        return ReportResponseDto.ObjectiveMetricsDto.builder()
                .marketAnalysis(marketAnalysis)
                .averageMarketDeposit(avgDeposit)
                .averageMarketRent(avgMonthlyRent)
                .userDeposit(userDeposit != null ? userDeposit.doubleValue() : 0.0)
                .userRent(userRent != null ? userRent.doubleValue() : 0.0)
                .priceComparison(calculatePriceComparison(userDeposit, userRent, avgDeposit, avgMonthlyRent))
                .neighborhoodComparisons(neighborhoodComparisons)
                .dataSource("공공데이터포털 실거래가 API")
                .lastUpdated(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .build();
    }
    
    /**
     * 시장 분석 텍스트 생성
     */
    private String generateMarketAnalysis(Long userDeposit, Integer userRent, double avgDeposit, double avgMonthlyRent) {
        if (userDeposit == null || userRent == null) {
            return "계약 조건 정보가 없어 시세 비교가 어렵습니다.";
        }
        
        double depositDiff = userDeposit - avgDeposit;
        double rentDiff = userRent - avgMonthlyRent;
        
        StringBuilder analysis = new StringBuilder();
        
        if (Math.abs(depositDiff) < avgDeposit * 0.1) {
            analysis.append("보증금은 시세와 비슷한 수준입니다. ");
        } else if (depositDiff > 0) {
            analysis.append(String.format("보증금이 시세보다 %.0f만원 높습니다. ", depositDiff / 10000));
        } else {
            analysis.append(String.format("보증금이 시세보다 %.0f만원 낮습니다. ", Math.abs(depositDiff) / 10000));
        }
        
        if (Math.abs(rentDiff) < avgMonthlyRent * 0.1) {
            analysis.append("월세도 시세와 비슷한 수준입니다.");
        } else if (rentDiff > 0) {
            analysis.append(String.format("월세가 시세보다 %.0f만원 높습니다.", rentDiff / 10000));
        } else {
            analysis.append(String.format("월세가 시세보다 %.0f만원 낮습니다.", Math.abs(rentDiff) / 10000));
        }
        
        return analysis.toString();
    }
    
    /**
     * 가격 비교 분석
     */
    private String calculatePriceComparison(Long userDeposit, Integer userRent, double avgDeposit, double avgMonthlyRent) {
        if (userDeposit == null || userRent == null) {
            return "계약 조건 정보 부족";
        }
        
        double depositRatio = (userDeposit / avgDeposit - 1) * 100;
        double rentRatio = (userRent / avgMonthlyRent - 1) * 100;
        
        if (depositRatio > 10 && rentRatio > 10) {
            return "시세 대비 높음";
        } else if (depositRatio < -10 && rentRatio < -10) {
            return "시세 대비 낮음";
        } else {
            return "시세 대비 적정";
        }
    }
    
    /**
     * 목업 객관적 지표 생성 (API 실패 시)
     */
    private ReportResponseDto.ObjectiveMetricsDto createMockObjectiveMetrics(Member member) {
        Long userDeposit = member.getSecurity();
        Integer userRent = member.getRent();
        
        // 사용자 주소에 따른 동적 목업 데이터 생성
        String dong = member.getDong() != null ? member.getDong() : "알 수 없는 동";
        String buildingType = member.getBuildingType() != null ? member.getBuildingType() : "오피스텔";
        
        // 건물 유형과 지역에 따른 시세 데이터 생성
        double baseDeposit = 50000000.0; // 5000만원 기본값
        double baseRent = 800000.0; // 80만원 기본값
        
        // 건물 유형별 가격 조정
        if (buildingType.contains("오피스텔")) {
            baseDeposit = 60000000.0; // 6000만원
            baseRent = 900000.0; // 90만원
        } else if (buildingType.contains("빌라") || buildingType.contains("다세대")) {
            baseDeposit = 45000000.0; // 4500만원
            baseRent = 700000.0; // 70만원
        }
        
        // 지역별 가격 조정 (간단한 예시)
        if (dong.contains("강남") || dong.contains("서초")) {
            baseDeposit *= 1.3;
            baseRent *= 1.3;
        } else if (dong.contains("마포") || dong.contains("용산")) {
            baseDeposit *= 1.1;
            baseRent *= 1.1;
        }
        
        String marketAnalysis = generateMarketAnalysis(userDeposit, userRent, baseDeposit, baseRent);
        
        // 동네별 비교 데이터 생성
        List<ReportResponseDto.NeighborhoodComparisonDto> mockComparisons = Arrays.asList(
            ReportResponseDto.NeighborhoodComparisonDto.builder()
                .neighborhoodName(dong + " 인근 지역 1")
                .averageDeposit(baseDeposit * 0.9)
                .averageMonthlyRent(baseRent * 0.9)
                .transactionCount(12)
                .build(),
            ReportResponseDto.NeighborhoodComparisonDto.builder()
                .neighborhoodName(dong + " 인근 지역 2")
                .averageDeposit(baseDeposit * 1.1)
                .averageMonthlyRent(baseRent * 1.1)
                .transactionCount(8)
                .build(),
            ReportResponseDto.NeighborhoodComparisonDto.builder()
                .neighborhoodName(dong + " 인근 지역 3")
                .averageDeposit(baseDeposit * 0.95)
                .averageMonthlyRent(baseRent * 0.95)
                .transactionCount(15)
                .build()
        );
        
        return ReportResponseDto.ObjectiveMetricsDto.builder()
                .marketAnalysis(marketAnalysis)
                .averageMarketDeposit(baseDeposit)
                .averageMarketRent(baseRent)
                .userDeposit(userDeposit != null ? userDeposit.doubleValue() : 0.0)
                .userRent(userRent != null ? userRent.doubleValue() : 0.0)
                .priceComparison(calculatePriceComparison(userDeposit, userRent, baseDeposit, baseRent))
                .neighborhoodComparisons(mockComparisons)
                .dataSource("시뮬레이션 데이터 (실제 시세 참고)")
                .lastUpdated(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .build();
    }

    /**
     * 공유 가능한 URL 생성
     */
    public String generateShareUrl(String reportId, boolean isPremium) {
        try {
            System.out.println("DEBUG - generateShareUrl called with reportId: " + reportId + ", isPremium: " + isPremium);
            
            // reportId가 null이거나 빈 문자열인 경우 기본 URL 반환
            if (reportId == null || reportId.trim().isEmpty()) {
                System.out.println("DEBUG - reportId is null or empty, returning default URL");
                String baseUrl = "https://rental-lovat-theta.vercel.app";
                return baseUrl + "/report";
            }
            
            // 리포트 ID로 리포트 조회
            Report report = reportRepository.findByPublicId(reportId)
                    .orElseThrow(() -> new RuntimeException("리포트를 찾을 수 없습니다: " + reportId));
            
            System.out.println("DEBUG - Report found: " + report.getReportId());
            
            // 공유 가능한 URL 생성 (프론트엔드 도메인 + 공개 경로)
            String baseUrl = "https://rental-lovat-theta.vercel.app";
            String sharePath = isPremium ? 
                    "/report/" + reportId + "?type=premium" : 
                    "/report/" + reportId;
            
            String shareUrl = baseUrl + sharePath;
            System.out.println("DEBUG - Generated share URL: " + shareUrl);
            
            return shareUrl;
        } catch (Exception e) {
            System.out.println("ERROR - generateShareUrl failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("공유 URL 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}