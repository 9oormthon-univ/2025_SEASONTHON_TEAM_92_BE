package org.example.seasontonebackend.policy.dto;

import lombok.*;
import java.util.List;

public class RentalLawResponseDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ArticleList {
        private List<ArticleDetail> articles;
        private Integer totalCount;
        private String searchKeyword;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ArticleDetail {
        private Long articleId;
        private String articleNumber;
        private String articleTitle;
        private String articleContent;
        private String category;
        private List<String> applicableSituations;
        private List<String> relatedKeywords;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ApiResponse<T> {
        private Boolean success;
        private T data;
        private String message;

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, data, message);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, null, message);
        }
    }
}