package com.yachaerang.backend.api.article.dto.response;

import com.yachaerang.backend.api.article.dto.request.ArticleRequestDto;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class ArticleResponseDto {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PageDto<T> {
        private List<T> content;
        private int currentPage;
        private int pageSize;
        private Long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;

        public static <T> PageDto<T> of(
                List<T> content,
                ArticleRequestDto.PageDto pageRequest,
                long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / pageRequest.getSize());
            int currentPage = pageRequest.getPage();

            return PageDto.<T>builder()
                    .content(content)
                    .currentPage(currentPage)
                    .pageSize(pageRequest.getSize())
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .first(currentPage == 1)
                    .last(currentPage >= totalPages)
                    .hasNext(currentPage < totalPages)
                    .hasPrevious(currentPage > 1)
                    .build();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DetailDto {
        private Long articleId;
        private String title;
        private String content;
        private String imageUrl;
        private String url;
        private List<String> tagList;
        private LocalDate createdAt;
        private Boolean isBookmarked;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ListDto {
        private Long articleId;
        private String title;
        private String imageUrl;
        private List<String> tagList;
        private LocalDate createdAt;
        private Boolean isBookmarked;
    }
}
