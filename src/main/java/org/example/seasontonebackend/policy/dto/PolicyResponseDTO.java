package org.example.seasontonebackend.policy.dto;

import lombok.*;
import java.util.List;

public class PolicyResponseDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PersonalizedPolicies {
        private List<PolicyDetail> recommendedPolicies;
        private Integer totalCount;
        private List<CategorySummary> categories;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PolicyDetail {
        private Long policyId;
        private String policyName;
        private String category;
        private String summary;
        private String amountInfo;
        private Integer matchScore;
        private String eligibilityStatus;
        private String externalUrl;
        private Boolean isBookmarked;
        private List<String> tags;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CategorySummary {
        private String categoryName;
        private Integer count;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PolicyDetailResponse {
        private Long policyId;
        private String policyName;
        private String category;
        private String description;
        private TargetAudience targetAudience;
        private List<String> eligibilityCriteria;
        private String applicationMethod;
        private List<String> requiredDocuments;
        private ContactInfo contactInfo;
        private String amountInfo;
        private String applicationPeriod;
        private UserEligibilityCheck userEligibilityCheck;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TargetAudience {
        private String ageRange;
        private String incomeCriteria;
        private String region;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ContactInfo {
        private String phone;
        private String website;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserEligibilityCheck {
        private Boolean isEligible;
        private Integer matchScore;
        private List<String> unmetCriteria;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SituationInfoCard {
        private String situation;
        private List<PolicySummary> personalizedPolicies;
        private List<LawArticleSummary> relatedLawArticles;
        private List<AgencySummary> recommendedAgencies;
        private List<String> actionGuide;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PolicySummary {
        private Long policyId;
        private String policyName;
        private String summary;
        private Integer matchScore;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LawArticleSummary {
        private Long articleId;
        private String articleNumber;
        private String articleTitle;
        private List<String> keyPoints;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AgencySummary {
        private Long agencyId;
        private String agencyName;
        private String reason;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ApiResponse<T> {
        private Boolean success;
        private T data;
        private String message;

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, data, message);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, null, message);
        }
    }
}