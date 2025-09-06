package org.example.seasontonebackend.policy.dto;

import lombok.*;
import java.util.List;

public class DisputeAgencyResponseDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AgencyList {
        private List<AgencyDetail> agencies;
        private String userRegion;
        private Integer totalCount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AgencyDetail {
        private Long agencyId;
        private String agencyName;
        private String agencyType;
        private String description;
        private String jurisdiction;
        private ContactInfo contactInfo;
        private String operatingHours;
        private List<String> serviceTypes;
        private String processingTime;
        private String costInfo;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ContactInfo {
        private String phone;
        private String address;
        private String website;
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