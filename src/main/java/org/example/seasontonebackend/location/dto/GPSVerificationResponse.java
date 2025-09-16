package org.example.seasontonebackend.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPS 인증 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GPSVerificationResponse {
    
    private boolean isVerified;
    private Integer confidence; // 0-100
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Long timestamp;
    private String address;
    private String dong;
    private String gu;
    private String si;
    private String verificationMethod; // "gps", "manual", "hybrid"
    private String verifiedAt;
    private String message;
}