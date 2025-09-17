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
public class PolicyService {

    public PolicyResponseDTO.PersonalizedPolicies getPersonalizedPolicies(Member member) {
        String userRegion = member.getDong();
        List<PolicyResponseDTO.PolicyDetail> policies = getPoliciesByUserRegion(userRegion);

        Map<String, Long> categoryCounts = new HashMap<>();
        categoryCounts.put("주거지원", 4L);
        categoryCounts.put("대출지원", 1L);
        categoryCounts.put("지역지원", 2L);

        List<PolicyResponseDTO.CategorySummary> categories = Arrays.asList(
                PolicyResponseDTO.CategorySummary.builder().categoryName("주거지원").count(4).build(),
                PolicyResponseDTO.CategorySummary.builder().categoryName("대출지원").count(1).build(),
                PolicyResponseDTO.CategorySummary.builder().categoryName("지역지원").count(2).build()
        );

        return PolicyResponseDTO.PersonalizedPolicies.builder()
                .recommendedPolicies(policies)
                .totalCount(policies.size())
                .categories(categories)
                .build();
    }

    public PolicyResponseDTO.PersonalizedPolicies getPoliciesByCategory(String categoryCode, Member member) {
        String userRegion = member.getDong();
        List<PolicyResponseDTO.PolicyDetail> allPolicies = getPoliciesByUserRegion(userRegion);

        List<PolicyResponseDTO.PolicyDetail> filteredPolicies = allPolicies.stream()
                .filter(policy -> policy.getCategory().equals(categoryCode))
                .toList();

        List<PolicyResponseDTO.CategorySummary> categories = Arrays.asList(
                PolicyResponseDTO.CategorySummary.builder().categoryName(categoryCode).count(filteredPolicies.size()).build()
        );

        return PolicyResponseDTO.PersonalizedPolicies.builder()
                .recommendedPolicies(filteredPolicies)
                .totalCount(filteredPolicies.size())
                .categories(categories)
                .build();
    }

    public PolicyResponseDTO.PolicyDetailResponse getPolicyDetail(Long policyId, Member member) {
        // 하드코딩된 정책 상세 정보
        return PolicyResponseDTO.PolicyDetailResponse.builder()
                .policyId(policyId)
                .policyName("청년 월세 한시 특별지원")
                .category("주거지원")
                .description("만 19~34세 청년층의 주거비 부담 완화를 위한 지원사업으로, 월세 부담을 줄여 청년들의 주거 안정을 도모합니다.")
                .targetAudience(PolicyResponseDTO.TargetAudience.builder()
                        .ageRange("19-34세")
                        .incomeCriteria("중위소득 150% 이하")
                        .region("전국")
                        .build())
                .eligibilityCriteria(Arrays.asList(
                        "만 19~34세 무주택자",
                        "부모와 별도 거주",
                        "중위소득 150% 이하"
                ))
                .applicationMethod("온라인 신청 (복지로)")
                .requiredDocuments(Arrays.asList(
                        "임대차계약서",
                        "소득증명서류",
                        "주민등록등본"
                ))
                .contactInfo(PolicyResponseDTO.ContactInfo.builder()
                        .phone("1600-0000")
                        .website("https://www.molit.go.kr")
                        .build())
                .amountInfo("최대 월 20만원, 12개월 지원")
                .applicationPeriod("2025년 1월 ~ 12월")
                .userEligibilityCheck(PolicyResponseDTO.UserEligibilityCheck.builder()
                        .isEligible(true)
                        .matchScore(95)
                        .unmetCriteria(Arrays.asList())
                        .build())
                .build();
    }

    public void bookmarkPolicy(Long policyId, Member member) {
        log.info("정책 북마크 - 정책ID: {}, 사용자: {}", policyId, member.getEmail());
    }

    public void unbookmarkPolicy(Long policyId, Member member) {
        log.info("정책 북마크 해제 - 정책ID: {}, 사용자: {}", policyId, member.getEmail());
    }

    public void applyPolicy(Long policyId, Member member) {
        log.info("정책 신청 - 정책ID: {}, 사용자: {}", policyId, member.getEmail());
    }

    private List<PolicyResponseDTO.PolicyDetail> getPoliciesByUserRegion(String userRegion) {
        List<PolicyResponseDTO.PolicyDetail> allPolicies = Arrays.asList(
                // 전국 정책
                PolicyResponseDTO.PolicyDetail.builder()
                        .policyId(1L)
                        .policyName("청년 주거 지원 정책")
                        .category("주거지원")
                        .summary("청년층의 주거 안정을 위한 다양한 지원 정책")
                        .amountInfo("월 최대 30만원")
                        .matchScore(95)
                        .eligibilityStatus("자격 충족")
                        .externalUrl("https://www.bokjiro.go.kr/ssis-tbu/twataa/wlfareInfo/moveTWAT52011M.do?wlfareInfoId=WLF00005696")
                        .isBookmarked(false)
                        .tags(Arrays.asList("청년", "월세지원", "소득기준"))
                        .build(),

                PolicyResponseDTO.PolicyDetail.builder()
                        .policyId(2L)
                        .policyName("국민주택기금 청년 지원")
                        .category("대출지원")
                        .summary("국민주택기금을 통한 청년 주거 지원 사업")
                        .amountInfo("월 최대 40만원")
                        .matchScore(88)
                        .eligibilityStatus("자격 충족")
                        .externalUrl("https://nhuf.molit.go.kr/FP/FP05/FP0502/FP05020701.jsp")
                        .isBookmarked(false)
                        .tags(Arrays.asList("청년", "대출", "보증금"))
                        .build(),

                // 서울시 정책
                PolicyResponseDTO.PolicyDetail.builder()
                        .policyId(3L)
                        .policyName("서울시 청년 주거 지원")
                        .category("주거지원")
                        .summary("서울시 청년을 위한 맞춤형 주거 지원 프로그램")
                        .amountInfo("월 최대 50만원")
                        .matchScore(92)
                        .eligibilityStatus("자격 충족")
                        .externalUrl("https://housing.seoul.go.kr/site/main/content/sh01_070400#non")
                        .isBookmarked(false)
                        .tags(Arrays.asList("서울시", "청년", "주거"))
                        .build(),

                PolicyResponseDTO.PolicyDetail.builder()
                        .policyId(4L)
                        .policyName("서울시 청년 주거환경 개선 지원")
                        .category("주거지원")
                        .summary("청년 대상 곰팡이, 누수 등 주거환경 개선 공사비 지원")
                        .amountInfo("최대 150만원")
                        .matchScore(85)
                        .eligibilityStatus("자격 충족")
                        .externalUrl("https://seoul.go.kr/youth-housing-improve")
                        .isBookmarked(false)
                        .tags(Arrays.asList("서울시", "청년", "주거환경"))
                        .build(),

                // 마포구 정책
                PolicyResponseDTO.PolicyDetail.builder()
                        .policyId(5L)
                        .policyName("마포구 청년 임차보증금 지원")
                        .category("지역지원")
                        .summary("마포구 거주 청년 임차보증금 무이자 대출")
                        .amountInfo("최대 500만원, 무이자")
                        .matchScore(98)
                        .eligibilityStatus("자격 충족")
                        .externalUrl("https://mapo.go.kr/youth-deposit")
                        .isBookmarked(false)
                        .tags(Arrays.asList("마포구", "청년", "보증금"))
                        .build(),

                PolicyResponseDTO.PolicyDetail.builder()
                        .policyId(6L)
                        .policyName("마포구 청년 집수리 지원")
                        .category("지역지원")
                        .summary("마포구 거주 청년 대상 주택 수선 지원")
                        .amountInfo("최대 100만원")
                        .matchScore(90)
                        .eligibilityStatus("자격 충족")
                        .externalUrl("https://mapo.go.kr/youth-repair")
                        .isBookmarked(false)
                        .tags(Arrays.asList("마포구", "청년", "수선"))
                        .build(),

                // 강남구 정책
                PolicyResponseDTO.PolicyDetail.builder()
                        .policyId(7L)
                        .policyName("강남구 청년 주거안정 지원")
                        .category("지역지원")
                        .summary("강남구 거주 청년 대상 주거비 지원")
                        .amountInfo("최대 월 10만원, 12개월")
                        .matchScore(87)
                        .eligibilityStatus("자격 충족")
                        .externalUrl("https://gangnam.go.kr/youth")
                        .isBookmarked(false)
                        .tags(Arrays.asList("강남구", "청년", "주거"))
                        .build()
        );

        // 사용자 지역에 따른 필터링
        return allPolicies.stream()
                .filter(policy -> isPolicyApplicableToRegion(policy, userRegion))
                .sorted((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()))
                .toList();
    }

    private boolean isPolicyApplicableToRegion(PolicyResponseDTO.PolicyDetail policy, String userRegion) {
        if (userRegion == null) return true;

        if (userRegion.contains("마포")) {
            return policy.getTags().contains("마포구") ||
                    policy.getTags().contains("서울시") ||
                    policy.getTags().contains("청년");
        } else if (userRegion.contains("강남")) {
            return policy.getTags().contains("강남구") ||
                    policy.getTags().contains("서울시") ||
                    policy.getTags().contains("청년");
        } else if (userRegion.contains("서울")) {
            return policy.getTags().contains("서울시") ||
                    policy.getTags().contains("청년");
        }

        return policy.getTags().contains("청년");
    }
}