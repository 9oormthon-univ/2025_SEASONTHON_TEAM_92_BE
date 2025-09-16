package org.example.seasontonebackend.policy.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.policy.application.RentalLawService;
import org.example.seasontonebackend.policy.dto.RentalLawResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rental-law")
@RequiredArgsConstructor
@Slf4j
public class RentalLawController {

    private final RentalLawService rentalLawService;

    // 1. 상황별 관련 조항 조회
    @GetMapping("/articles")
    public ResponseEntity<RentalLawResponseDTO.ApiResponse<RentalLawResponseDTO.ArticleList>> getArticlesBySituation(
            @RequestParam(required = false) String situation,
            @RequestParam(required = false) String keyword) {

        try {
            RentalLawResponseDTO.ArticleList response = rentalLawService.getArticlesBySituation(situation, keyword);
            String message = situation != null ?
                    situation + " 관련 법령 조항을 조회했습니다." :
                    "법령 조항을 조회했습니다.";

            return ResponseEntity.ok(RentalLawResponseDTO.ApiResponse.success(response, message));

        } catch (Exception e) {
            log.error("법령 조항 조회 실패 - 상황: {}, 키워드: {}, 오류: {}", situation, keyword, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(RentalLawResponseDTO.ApiResponse.error("법령 조항 조회 중 오류가 발생했습니다."));
        }
    }

    // 2. 카테고리별 조항 조회
    @GetMapping("/articles/categories/{category}")
    public ResponseEntity<RentalLawResponseDTO.ApiResponse<RentalLawResponseDTO.ArticleList>> getArticlesByCategory(
            @PathVariable String category) {

        try {
            RentalLawResponseDTO.ArticleList response = rentalLawService.getArticlesByCategory(category);
            return ResponseEntity.ok(RentalLawResponseDTO.ApiResponse.success(response,
                    category + " 카테고리 법령 조항을 조회했습니다."));

        } catch (Exception e) {
            log.error("카테고리별 법령 조항 조회 실패 - 카테고리: {}, 오류: {}", category, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(RentalLawResponseDTO.ApiResponse.error("법령 조항 조회 중 오류가 발생했습니다."));
        }
    }
}