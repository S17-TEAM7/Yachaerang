package com.yachaerang.backend.api.bookmark.dto.response;

import lombok.*;

import java.util.List;

public class BookmarkResponseDto {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GetAllDto {
        private Long bookmarkId;
        private Long articleId;
        private String title;
        private List<String> tagList;
    }
}
