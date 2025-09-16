package org.example.seasontonebackend.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ReportResponseDto {

    // 리포트 타입 (무료/프리미엄)
    private String reportType;

    // 1. 리포트 헤더 (기획안)
    private ReportHeaderDto header;

    // 2. 나의 계약 정보 요약 (기획안 STEP 1-3)
    private ContractSummaryDto contractSummary;

    // 3. 주관적 지표 (기획안 STEP 2)
    private SubjectiveMetricsDto subjectiveMetrics;

    // 4. 객관적 지표 (기획안 STEP 4-1) - 추후 구현 예정
    // private ObjectiveMetricsDto objectiveMetrics;

    // 5. 협상 카드 (기획안 STEP 3-3)
    private List<NegotiationCardDto> negotiationCards;

    // 6. 맞춤형 정책 정보 (기획안 STEP 4-2)
    private List<PolicyInfoDto> policyInfos;

    // 7. 분쟁 해결 가이드 (기획안 STEP 4-3)
    private DisputeGuideDto disputeGuide;

    // 8. 스마트 진단 데이터 (프리미엄 전용)
    private Object smartDiagnosisData;

    // --- 각 DTO의 상세 구조 ---

    @Getter
    @Builder
    public static class ReportHeaderDto {
        private String title; // "망원동 ○○빌라 임대차 협상 리포트"
        private String generatedDate; // "2025.09.08"
        private String dataPeriod; // "본 리포트는 최근 1개월 내 참여자 데이터..."
        private int participantCount; // 참여 인원 수 (e.g., 15)
        private String dataRecency; // 데이터 최신성 (e.g., "평균 응답 23일 전")
        private int reliabilityScore; // 신뢰도 점수 (e.g., 87)
    }

    @Getter
    @Builder
    public static class ContractSummaryDto {
        private String address; // 주소/건물명
        private String buildingType; // 건물 유형
        private String contractType; // 계약 유형
        private String conditions; // 조건 (e.g., "보증금 1,000 / 월세 60")
        private boolean gpsVerified; // GPS 인증 여부
        private boolean contractVerified; // 계약서 인증 여부
    }

    @Getter
    @Builder
    public static class SubjectiveMetricsDto {
        private ScoreComparison overallScore; // 종합 점수 비교
        private List<ScoreComparison> categoryScores; // 카테고리별 점수 비교 (채광, 방음 등)
    }

    @Getter
    @Builder
    public static class ScoreComparison {
        private String category; // "종합", "채광", "방음" 등
        private double myScore;
        private double buildingAverage;
        private double neighborhoodAverage;
    }

    // 객관적 지표 DTO - 추후 필드 정의
    @Getter
    @Builder
    public static class ObjectiveMetricsDto {
        // 예: private PriceComparison marketPrice;
    }

    @Getter
    @Builder
    public static class NegotiationCardDto {
        private int priority; // 1순위, 2순위...
        private String title; // "시설 개선 요구", "월세 조정 요구"
        private String recommendationScript; // "수압 문제는 우리 건물 평균 대비..."
        
        // 프리미엄 전용 필드들
        private String successProbability; // 성공 확률 (예: "85%")
        private String alternativeStrategy; // 대체 전략
        private String expertTip; // 전문가 팁
    }

    @Getter
    @Builder
    public static class PolicyInfoDto {
        private String title; // "청년 월세 특별지원"
        private String description; // "국토부, 신청 조건..."
        private String link; // 신청 링크
        
        // 프리미엄 전용 필드들
        private Boolean isEligible; // 자동 매칭 결과 (true/false)
        private String applicationDeadline; // 신청 마감일
        private List<String> requiredDocuments; // 필요 서류 목록
    }

    @Getter
    @Builder
    public static class DisputeGuideDto {
        private String relatedLaw; // "주택임대차보호법 ○○조 (임대인 수선 의무)"
        private String committeeInfo; // "서울서부 임대차분쟁조정위원회 연락처/링크"
        private String formDownloadLink; // "수선 요구서 템플릿 다운로드 링크"
        
        // 프리미엄 전용 필드들
        private List<DisputeRoadmapStepDto> disputeRoadmap; // 분쟁 해결 로드맵
        private ExpertConsultationDto expertConsultation; // 전문가 상담 정보
    }
    
    @Getter
    @Builder
    public static class DisputeRoadmapStepDto {
        private int step; // 단계 번호
        private String title; // 단계 제목
        private String description; // 단계 설명
        private String estimatedTime; // 예상 소요 시간
        private String cost; // 비용
    }
    
    @Getter
    @Builder
    public static class ExpertConsultationDto {
        private boolean available; // 상담 가능 여부
        private int price; // 상담 비용
        private String duration; // 상담 시간
        private String contactInfo; // 연락처
    }
}