package org.example.seasontonebackend.policy.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.policy.application.PolicyService;
import org.example.seasontonebackend.policy.dto.PolicyResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyService policyService;

    // 1. 사용자 맞춤형 정책 정보 API
    @GetMapping("/personalized")
    public ResponseEntity<PolicyResponseDTO.ApiResponse<PolicyResponseDTO.PersonalizedPolicies>> getPersonalizedPolicies(
            @AuthenticationPrincipal Member member) {

        try {
            PolicyResponseDTO.PersonalizedPolicies response = policyService.getPersonalizedPolicies(member);
            return ResponseEntity.ok(PolicyResponseDTO.ApiResponse.success(response, "맞춤형 정책을 조회했습니다."));

        } catch (Exception e) {
            log.error("맞춤형 정책 조회 실패 - 사용자: {}, 오류: {}", member.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(PolicyResponseDTO.ApiResponse.error("정책 조회 중 오류가 발생했습니다."));
        }
    }

    // 2. 정책 카테고리별 조회
    @GetMapping("/categories/{categoryCode}")
    public ResponseEntity<PolicyResponseDTO.ApiResponse<PolicyResponseDTO.PersonalizedPolicies>> getPoliciesByCategory(
            @PathVariable String categoryCode,
            @AuthenticationPrincipal Member member) {

        try {
            PolicyResponseDTO.PersonalizedPolicies response = policyService.getPoliciesByCategory(categoryCode, member);
            return ResponseEntity.ok(PolicyResponseDTO.ApiResponse.success(response,
                    categoryCode + " 카테고리 정책을 조회했습니다."));

        } catch (Exception e) {
            log.error("카테고리별 정책 조회 실패 - 카테고리: {}, 사용자: {}, 오류: {}",
                    categoryCode, member.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(PolicyResponseDTO.ApiResponse.error("정책 조회 중 오류가 발생했습니다."));
        }
    }

    // 3. 정책 상세 조회
    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyResponseDTO.ApiResponse<PolicyResponseDTO.PolicyDetailResponse>> getPolicyDetail(
            @PathVariable Long policyId,
            @AuthenticationPrincipal Member member) {

        try {
            PolicyResponseDTO.PolicyDetailResponse response = policyService.getPolicyDetail(policyId, member);
            return ResponseEntity.ok(PolicyResponseDTO.ApiResponse.success(response, "정책 상세정보를 조회했습니다."));

        } catch (Exception e) {
            log.error("정책 상세 조회 실패 - 정책ID: {}, 사용자: {}, 오류: {}",
                    policyId, member.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(PolicyResponseDTO.ApiResponse.error("정책 조회 중 오류가 발생했습니다."));
        }
    }

    // 4-1. 정책 북마크 추가
    @PostMapping("/{policyId}/bookmark")
    public ResponseEntity<PolicyResponseDTO.ApiResponse<String>> bookmarkPolicy(
            @PathVariable Long policyId,
            @AuthenticationPrincipal Member member) {

        try {
            policyService.bookmarkPolicy(policyId, member);
            return ResponseEntity.ok(PolicyResponseDTO.ApiResponse.success("success", "정책이 북마크되었습니다."));

        } catch (Exception e) {
            log.error("정책 북마크 실패 - 정책ID: {}, 사용자: {}, 오류: {}",
                    policyId, member.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(PolicyResponseDTO.ApiResponse.error("북마크 처리 중 오류가 발생했습니다."));
        }
    }

    // 4-2. 정책 북마크 제거
    @DeleteMapping("/{policyId}/bookmark")
    public ResponseEntity<PolicyResponseDTO.ApiResponse<String>> unbookmarkPolicy(
            @PathVariable Long policyId,
            @AuthenticationPrincipal Member member) {

        try {
            policyService.unbookmarkPolicy(policyId, member);
            return ResponseEntity.ok(PolicyResponseDTO.ApiResponse.success("success", "북마크가 해제되었습니다."));

        } catch (Exception e) {
            log.error("정책 북마크 해제 실패 - 정책ID: {}, 사용자: {}, 오류: {}",
                    policyId, member.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(PolicyResponseDTO.ApiResponse.error("북마크 해제 중 오류가 발생했습니다."));
        }
    }

    // 4-3. 정책 신청
    @PostMapping("/{policyId}/apply")
    public ResponseEntity<PolicyResponseDTO.ApiResponse<String>> applyPolicy(
            @PathVariable Long policyId,
            @AuthenticationPrincipal Member member) {

        try {
            policyService.applyPolicy(policyId, member);
            return ResponseEntity.ok(PolicyResponseDTO.ApiResponse.success("success", "정책 신청이 완료되었습니다."));

        } catch (Exception e) {
            log.error("정책 신청 실패 - 정책ID: {}, 사용자: {}, 오류: {}",
                    policyId, member.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(PolicyResponseDTO.ApiResponse.error("정책 신청 중 오류가 발생했습니다."));
        }
    }
}