package org.example.seasontonebackend.policy.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.policy.dto.RentalLawResponseDTO;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalLawService {

    public RentalLawResponseDTO.ArticleList getArticlesBySituation(String situation, String keyword) {
        List<RentalLawResponseDTO.ArticleDetail> allArticles = getAllArticles();
        String searchKeyword = situation != null ? situation : (keyword != null ? keyword : "전체");

        List<RentalLawResponseDTO.ArticleDetail> filteredArticles;

        if (situation != null) {
            filteredArticles = allArticles.stream()
                    .filter(article -> article.getApplicableSituations().contains(situation))
                    .toList();
        } else if (keyword != null) {
            filteredArticles = allArticles.stream()
                    .filter(article ->
                            article.getArticleTitle().contains(keyword) ||
                                    article.getArticleContent().contains(keyword)
                    )
                    .toList();
        } else {
            filteredArticles = allArticles;
        }

        return RentalLawResponseDTO.ArticleList.builder()
                .articles(filteredArticles)
                .totalCount(filteredArticles.size())
                .searchKeyword(searchKeyword)
                .build();
    }

    public RentalLawResponseDTO.ArticleList getArticlesByCategory(String category) {
        List<RentalLawResponseDTO.ArticleDetail> allArticles = getAllArticles();

        List<RentalLawResponseDTO.ArticleDetail> filteredArticles = allArticles.stream()
                .filter(article -> article.getCategory().equals(category))
                .toList();

        return RentalLawResponseDTO.ArticleList.builder()
                .articles(filteredArticles)
                .totalCount(filteredArticles.size())
                .searchKeyword(category)
                .build();
    }

    private List<RentalLawResponseDTO.ArticleDetail> getAllArticles() {
        return Arrays.asList(
                RentalLawResponseDTO.ArticleDetail.builder()
                        .articleId(1L)
                        .articleNumber("제20조")
                        .articleTitle("임대인의 수선의무")
                        .articleContent("임대인은 임대목적물을 사용·수익에 필요한 상태로 유지하게 할 의무를 진다...")
                        .category("수선의무")
                        .applicableSituations(Arrays.asList("곰팡이", "누수", "시설고장"))
                        .relatedKeywords(Arrays.asList("수선", "수리", "하자", "곰팡이"))
                        .build(),

                RentalLawResponseDTO.ArticleDetail.builder()
                        .articleId(2L)
                        .articleNumber("제7조")
                        .articleTitle("임대료 증액 제한")
                        .articleContent("임대료는 전 계약 대비 연 5% 범위 내에서만 증액 가능...")
                        .category("임대료")
                        .applicableSituations(Arrays.asList("임대료인상", "재계약"))
                        .relatedKeywords(Arrays.asList("인상", "5%", "제한"))
                        .build(),

                RentalLawResponseDTO.ArticleDetail.builder()
                        .articleId(3L)
                        .articleNumber("제10조의2")
                        .articleTitle("보증금 반환 의무")
                        .articleContent("임대인은 계약 종료 시 보증금을 즉시 반환해야 하며, 늦을 경우 연 12% 이자 지급...")
                        .category("보증금")
                        .applicableSituations(Arrays.asList("보증금반환", "계약해지"))
                        .relatedKeywords(Arrays.asList("반환", "12%", "이자"))
                        .build()
        );
    }
}