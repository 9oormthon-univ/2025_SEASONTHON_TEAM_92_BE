package org.example.seasontonebackend.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPS 인증 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GPSVerificationRequest {
    
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Long timestamp;
    private String targetAddress;
    private Double toleranceRadius; // 미터 단위 허용 오차
}