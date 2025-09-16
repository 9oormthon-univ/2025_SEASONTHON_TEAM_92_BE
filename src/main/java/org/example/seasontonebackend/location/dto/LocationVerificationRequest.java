package org.example.seasontonebackend.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPS 위치 인증 요청 DTO
 * userId는 JWT 토큰에서 자동 추출하므로 요청에 포함하지 않음
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationVerificationRequest {

    // userId는 JWT 토큰에서 추출하므로 제거
    private Double latitude;
    private Double longitude;
    private String buildingName;
}