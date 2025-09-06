package org.example.seasontonebackend.policy.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.policy.application.DisputeAgencyService;
import org.example.seasontonebackend.policy.dto.DisputeAgencyResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dispute-agencies")
@RequiredArgsConstructor
@Slf4j
public class DisputeAgencyController {

    private final DisputeAgencyService disputeAgencyService;

    // 1. 지역별 분쟁 해결 기관 조회
    @GetMapping
    public ResponseEntity<DisputeAgencyResponseDTO.ApiResponse<DisputeAgencyResponseDTO.AgencyList>> getAgenciesByRegion(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String agencyType,
            @AuthenticationPrincipal Member member) {

        try {
            String userRegion = region != null ? region : member.getDong();
            DisputeAgencyResponseDTO.AgencyList response = disputeAgencyService.getAgenciesByRegion(userRegion, agencyType);

            return ResponseEntity.ok(DisputeAgencyResponseDTO.ApiResponse.success(response,
                    userRegion + " 지역의 분쟁해결기관을 조회했습니다."));

        } catch (Exception e) {
            log.error("분쟁해결기관 조회 실패 - 지역: {}, 유형: {}, 사용자: {}, 오류: {}",
                    region, agencyType, member.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(DisputeAgencyResponseDTO.ApiResponse.error("분쟁해결기관 조회 중 오류가 발생했습니다."));
        }
    }

    // 2. 분쟁 유형별 기관 추천
    @GetMapping("/recommend")
    public ResponseEntity<DisputeAgencyResponseDTO.ApiResponse<DisputeAgencyResponseDTO.AgencyList>> getRecommendedAgencies(
            @RequestParam String disputeType,
            @AuthenticationPrincipal Member member) {

        try {
            DisputeAgencyResponseDTO.AgencyList response = disputeAgencyService.getRecommendedAgencies(disputeType, member);

            return ResponseEntity.ok(DisputeAgencyResponseDTO.ApiResponse.success(response,
                    disputeType + " 분쟁 해결에 적합한 기관을 추천했습니다."));

        } catch (Exception e) {
            log.error("분쟁해결기관 추천 실패 - 분쟁유형: {}, 사용자: {}, 오류: {}",
                    disputeType, member.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(DisputeAgencyResponseDTO.ApiResponse.error("기관 추천 중 오류가 발생했습니다."));
        }
    }
}