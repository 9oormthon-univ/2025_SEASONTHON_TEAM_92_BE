package org.example.seasontonebackend.policy.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.policy.dto.PolicyResponseDTO;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfoCardService {

    public PolicyResponseDTO.SituationInfoCard getSituationInfoCard(String situationType, Member member) {
        // 상황별 맞춤 정책
        List<PolicyResponseDTO.PolicySummary> policies = getSituationPolicies(situationType);

        // 관련 법령 조항
        List<PolicyResponseDTO.LawArticleSummary> lawArticles = getSituationLawArticles(situationType);

        // 추천 기관
        List<PolicyResponseDTO.AgencySummary> agencies = getSituationAgencies(situationType);

        // 행동 가이드
        List<String> actionGuide = getActionGuide(situationType);

        return PolicyResponseDTO.SituationInfoCard.builder()
                .situation(situationType)
                .personalizedPolicies(policies)
                .relatedLawArticles(lawArticles)
                .recommendedAgencies(agencies)
                .actionGuide(actionGuide)
                .build();
    }

    private List<PolicyResponseDTO.PolicySummary> getSituationPolicies(String situationType) {
        switch (situationType) {
            case "곰팡이":
                return Arrays.asList(
                        PolicyResponseDTO.PolicySummary.builder()
                                .policyId(4L)
                                .policyName("서울시 청년 주거환경 개선 지원")
                                .summary("곰팡이 제거 및 방수공사 지원")
                                .matchScore(88)
                                .build()
                );
            case "소음":
                return Arrays.asList(
                        PolicyResponseDTO.PolicySummary.builder()
                                .policyId(1L)
                                .policyName("청년 월세 한시 특별지원")
                                .summary("소음 문제로 인한 월세 부담 완화")
                                .matchScore(75)
                                .build()
                );
            default:
                return Arrays.asList(
                        PolicyResponseDTO.PolicySummary.builder()
                                .policyId(1L)
                                .policyName("청년 월세 한시 특별지원")
                                .summary("청년 주거비 부담 완화")
                                .matchScore(85)
                                .build()
                );
        }
    }

    private List<PolicyResponseDTO.LawArticleSummary> getSituationLawArticles(String situationType) {
        switch (situationType) {
            case "곰팡이":
                return Arrays.asList(
                        PolicyResponseDTO.LawArticleSummary.builder()
                                .articleId(1L)
                                .articleNumber("제20조")
                                .articleTitle("임대인의 수선의무")
                                .keyPoints(Arrays.asList("임대인은 곰팡이 제거 의무", "거주자 요청 시 즉시 수선"))
                                .build()
                );
            case "소음":
                return Arrays.asList(
                        PolicyResponseDTO.LawArticleSummary.builder()
                                .articleId(1L)
                                .articleNumber("제20조")
                                .articleTitle("임대인의 수선의무")
                                .keyPoints(Arrays.asList("방음 시설 개선 요구 가능", "생활 소음 해결 협의"))
                                .build()
                );
            default:
                return Arrays.asList(
                        PolicyResponseDTO.LawArticleSummary.builder()
                                .articleId(2L)
                                .articleNumber("제7조")
                                .articleTitle("임대료 증액 제한")
                                .keyPoints(Arrays.asList("연 5% 초과 인상 금지", "일방적 인상 무효"))
                                .build()
                );
        }
    }

    private List<PolicyResponseDTO.AgencySummary> getSituationAgencies(String situationType) {
        return Arrays.asList(
                PolicyResponseDTO.AgencySummary.builder()
                        .agencyId(1L)
                        .agencyName("서울특별시 임대차분쟁조정위원회")
                        .reason(situationType + " 관련 분쟁 전문 처리")
                        .build()
        );
    }

    private List<String> getActionGuide(String situationType) {
        switch (situationType) {
            case "곰팡이":
                return Arrays.asList(
                        "1. 곰팡이 발생 부위 사진 촬영",
                        "2. 임대인에게 서면 통지",
                        "3. 수선 요구 후 7일 대기",
                        "4. 미이행 시 분쟁조정위원회 신청"
                );
            case "소음":
                return Arrays.asList(
                        "1. 소음 발생 시간과 정도 기록",
                        "2. 이웃과 우선 대화 시도",
                        "3. 관리사무소에 신고",
                        "4. 지속될 경우 분쟁조정 신청"
                );
            default:
                return Arrays.asList(
                        "1. 상황 증거 수집",
                        "2. 임대인과 협의",
                        "3. 관련 기관 상담",
                        "4. 필요시 법적 조치"
                );
        }
    }
}