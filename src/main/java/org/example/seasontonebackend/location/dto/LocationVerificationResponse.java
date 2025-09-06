package org.example.seasontonebackend.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPS 위치 인증 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationVerificationResponse {

    private String userId;
    private String address;
    private String neighborhood;
    private String buildingName;
    private boolean verified;
    private String message;
}