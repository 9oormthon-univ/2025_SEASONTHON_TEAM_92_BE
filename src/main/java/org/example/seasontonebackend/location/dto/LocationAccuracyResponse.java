package org.example.seasontonebackend.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 위치 정확도 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationAccuracyResponse {
    
    private Double accuracy;
    private Integer confidence; // 0-100
    private List<String> recommendations;
    private String message;
}