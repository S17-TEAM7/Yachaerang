package com.yachaerang.backend.api.bookmark.vo;

import com.yachaerang.backend.api.article.entity.Article;
import lombok.Getter;
import lombok.Setter;

/**
 * Join 결과를 담기 위한 것
 */
@Getter
@Setter
public class ArticleBookmark {

    private Long bookmarkId;
    private Long memberId;
    private Long articleId;
    private Article article;
}
