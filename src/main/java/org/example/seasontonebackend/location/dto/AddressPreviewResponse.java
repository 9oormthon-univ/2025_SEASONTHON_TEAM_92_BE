package org.example.seasontonebackend.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주소 미리보기 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressPreviewResponse {
    private String address;
    private String neighborhood;
    private Double latitude;
    private Double longitude;
}