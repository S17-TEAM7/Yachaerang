package com.yachaerang.backend.api.article.entity;

import com.yachaerang.backend.api.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article extends BaseEntity {

    private Long id;

    private String title;

    private String content;

    private String contentHash;

    private String imageUrl;

    private Map<String, Object> imageMap;

    private int imageCount;

    private String url;

    private String sourceHost;

    private LocalDateTime fetchedAt;

    private Status status;

    private String errorMessage;

    private List<Tag> tagList;

    public enum Status {
        pending, success, failed
    }
}
