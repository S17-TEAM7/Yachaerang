package com.yachaerang.backend.api.bookmark.service;

import com.yachaerang.backend.api.article.entity.Article;
import com.yachaerang.backend.api.article.entity.Tag;
import com.yachaerang.backend.api.bookmark.dto.response.BookmarkResponseDto;
import com.yachaerang.backend.api.bookmark.vo.ArticleBookmark;
import com.yachaerang.backend.api.bookmark.entity.Bookmark;
import com.yachaerang.backend.api.bookmark.repository.BookmarkMapper;
import com.yachaerang.backend.global.auth.jwt.AuthenticatedMemberProvider;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final AuthenticatedMemberProvider authenticatedMemberProvider;
    private final BookmarkMapper bookmarkMapper;

    /**
     * 북마크 저장하기
     */
    @Transactional
    public void registerBookmark(Long articleId) {
        Long memberId = authenticatedMemberProvider.getCurrentMemberId();

        // 이미 등록된 것이라면 예외
        if (bookmarkMapper.findByMemberIdAndArticleId(memberId, articleId) != null) {
            throw GeneralException.of(ErrorCode.ALREADY_BOOKMARKED);
        }

        Bookmark bookmark = Bookmark.builder()
                .articleId(articleId)
                .memberId(memberId).build();
        int result = bookmarkMapper.save(bookmark);

        if (result != 1) {
            throw GeneralException.of(ErrorCode.BOOKMARK_DB_FAILED);
        }
        return;
    }

    /**
     * 북마크 해제하기
     */
    @Transactional
    public void eraseBookmark(Long articleId) {
        Long memberId = authenticatedMemberProvider.getCurrentMemberId();

        // 등록된 적이 없다면 예외
        if (bookmarkMapper.findByMemberIdAndArticleId(memberId, articleId) == null) {
        throw GeneralException.of(ErrorCode.BOOKMARK_NOT_FOUND);
        }

        // 북마크에서 해제
        int result = bookmarkMapper.delete(memberId, articleId);
        if (result != 1) {
            throw GeneralException.of(ErrorCode.BOOKMARK_DB_FAILED);
        }
        return;
    }

    /**
     * 북마크 전체 조회하기
     */
    @Transactional(readOnly = true)
    public List<BookmarkResponseDto.GetAllDto> getAll() {

        Long memberId = authenticatedMemberProvider.getCurrentMemberId();

        List<ArticleBookmark> bookmarkList = bookmarkMapper.findAllByMemberId(memberId);

        return bookmarkList.stream()
                .map(articleBookmark -> {
                    Article article = articleBookmark.getArticle();

                    return BookmarkResponseDto.GetAllDto.builder()
                            .bookmarkId(articleBookmark.getBookmarkId())
                            .articleId(articleBookmark.getArticleId())
                            .title(article.getTitle())
                            .tagList(
                                    article.getTagList() != null
                                    ? article.getTagList().stream()
                                            .map(Tag::getName)
                                            .toList()
                                            : Collections.emptyList()
                            )
                            .build();
                })
                .toList();
    }
}
