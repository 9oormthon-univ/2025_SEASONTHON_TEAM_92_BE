package org.example.seasontonebackend.policy.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.policy.application.InfoCardService;
import org.example.seasontonebackend.policy.dto.PolicyResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/info-cards")
@RequiredArgsConstructor
@Slf4j
public class InfoCardController {

    private final InfoCardService infoCardService;

    // 상황별 종합 정보 제공
    @GetMapping("/situation/{situationType}")
    public ResponseEntity<PolicyResponseDTO.ApiResponse<PolicyResponseDTO.SituationInfoCard>> getSituationInfoCard(
            @PathVariable String situationType,
            @AuthenticationPrincipal Member member) {

        try {
            PolicyResponseDTO.SituationInfoCard response = infoCardService.getSituationInfoCard(situationType, member);

            return ResponseEntity.ok(PolicyResponseDTO.ApiResponse.success(response,
                    situationType + " 상황에 대한 종합 정보를 제공합니다."));

        } catch (Exception e) {
            log.error("상황별 정보카드 조회 실패 - 상황유형: {}, 사용자: {}, 오류: {}",
                    situationType, member.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(PolicyResponseDTO.ApiResponse.error("정보 조회 중 오류가 발생했습니다."));
        }
    }
}