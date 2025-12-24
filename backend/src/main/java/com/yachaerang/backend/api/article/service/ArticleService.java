package com.yachaerang.backend.api.article.service;

import com.yachaerang.backend.api.article.dto.request.ArticleRequestDto;
import com.yachaerang.backend.api.article.dto.response.ArticleResponseDto;
import com.yachaerang.backend.api.article.entity.Article;
import com.yachaerang.backend.api.article.entity.Tag;
import com.yachaerang.backend.api.article.repository.ArticleMapper;
import com.yachaerang.backend.api.article.repository.TagMapper;
import com.yachaerang.backend.api.bookmark.repository.BookmarkMapper;
import com.yachaerang.backend.global.auth.jwt.AuthenticatedMemberProvider;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import com.yachaerang.backend.global.util.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    private final AuthenticatedMemberProvider authenticatedMemberProvider;

    private final ArticleMapper articleMapper;
    private final TagMapper tagMapper;
    private final BookmarkMapper bookmarkMapper;

    /**
     * 기사 목록 전체 조회하기
     * @param pageRequest : 페이지 요청 형식
     * @return : 페이지네이션 기반 기사 반환
     */
    public ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> getAllArticles(
            ArticleRequestDto.PageDto pageRequest) {

        // Article Pagination 조회
        List<Article> articleList = articleMapper.findAllWithPagination(pageRequest.getLimit(), pageRequest.getOffset());

        // 전체 개수 조회
        long totalElements = articleMapper.countAll();
        if (articleList.isEmpty()) {
            return ArticleResponseDto.PageDto.of(
                    Collections.emptyList(), pageRequest, totalElements
            );
        }

        // Article ID 목록
        List<Long> articleIdList = articleList.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        // 모든 Article의 TagName 한 번에 조회와 mapping
        Map<Long, List<String>> tagNameMap = getTagNameMap(articleIdList);

        LogUtil.debug("Articles found: {}", articleIdList);

        // 북마크된 ArticleID Set 조회
        Set<Long> bookmarkedArticleIdList = getBookmarkedArticleIds();

        List<ArticleResponseDto.ListDto> articleResponseDtoList
                = articleList.stream()
                .map(article -> ArticleResponseDto.ListDto.builder()
                        .articleId(article.getId())
                        .title(article.getTitle())
                        .imageUrl(article.getImageUrl())
                        .createdAt(article.getCreatedAt().toLocalDate())
                        .tagList(tagNameMap.getOrDefault(article.getId(), Collections.emptyList()))
                        .isBookmarked(bookmarkedArticleIdList.contains(article.getId()))
                        .build()
                )
                .toList();

        // 페이지네이션 Response 생성
        return ArticleResponseDto.PageDto.of(articleResponseDtoList, pageRequest, totalElements);
    }


    /*
    특정 기사만 조회하기
     */
    public ArticleResponseDto.DetailDto getArticle(Long articleId) {

        List<String> tagList = tagMapper.findNameByArticleId(articleId);
        Article article = articleMapper.findById(articleId);

        if (article == null) {
            throw GeneralException.of(ErrorCode.ARTICLE_NOT_FOUND);
        }

        // 북마크 여부 조회
        boolean isBookmarked = checkIfBookmarked(articleId);

        return ArticleResponseDto.DetailDto.builder()
                .articleId(articleId)
                .title(article.getTitle())
                .imageUrl(article.getImageUrl())
                .content(article.getContent())
                .url(article.getUrl())
                .tagList(tagList)
                .createdAt(article.getCreatedAt().toLocalDate())
                .isBookmarked(isBookmarked)
                .build();
    }

    /*
    기사 검색하기
     */
    public ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> searchArticles(
            ArticleRequestDto.PageDto pageRequest, String keyword) {

        LogUtil.info("{} 키워드 조회 시작...", keyword);
        List<Article> articleList = articleMapper.findByKeyword(pageRequest.getLimit(), pageRequest.getOffset(), keyword);

        // 페이지네이션을 위한 전체 조회
        long totalElements = articleMapper.countByKeyword(keyword);

        // 빈 리스트일 때
        if (articleList.isEmpty()) {
            return ArticleResponseDto.PageDto.of(
                    Collections.emptyList(), pageRequest, totalElements
            );
        }
        List<Long> articleIdList = articleList.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        // 모든 Article의 TagName 한 번에 조회와 mapping
        Map<Long, List<String>> tagNameMap = getTagNameMap(articleIdList);

        // 북마크된 ArticleID Set 조회
        Set<Long> bookmarkedArticleIdList = getBookmarkedArticleIds();

        List<ArticleResponseDto.ListDto> articleResponseDtoList
                = articleList.stream()
                .map(article -> ArticleResponseDto.ListDto.builder()
                        .articleId(article.getId())
                        .title(article.getTitle())
                        .imageUrl(article.getImageUrl())
                        .createdAt(article.getCreatedAt().toLocalDate())
                        .tagList(tagNameMap.getOrDefault(article.getId(), Collections.emptyList()))
                        .isBookmarked(bookmarkedArticleIdList.contains(article.getId()))
                        .build()
                )
                .toList();

        // 페이지네이션 Response 생성
        return ArticleResponseDto.PageDto.of(articleResponseDtoList, pageRequest, totalElements);
    }

    /**
     * articleIdList를 기반으로 tag를 매핑하여 반환
     * @param articleIdList : 매핑할 기사 ID 대상
     * @return : Map<기사ID, 그에대한 Tag의 Name 리스트>
     */
    private Map<Long, List<String>> getTagNameMap(List<Long> articleIdList) {
        List<Tag> tagList = tagMapper.findByArticleIdList(articleIdList);
        return tagList.stream()
                .peek(tag -> LogUtil.debug("스트림 처리 중 Tag: {}", tag))
                .collect(Collectors.groupingBy(
                        Tag::getArticleId,
                        Collectors.mapping(Tag::getName, Collectors.toList())
                ));
    }

    /**
     * 인증된 회원의 경우 북마크가 된 articleId를 반환
     * 인증되지 않은 회원의 경우 빈 리스트 반환
     * @return
     */
    private Set<Long> getBookmarkedArticleIds() {
        if (!authenticatedMemberProvider.isAuthenticated()) {
            return Collections.emptySet();
        }
        // 인증된 회원의 경우
        Long memberId = authenticatedMemberProvider.getCurrentMemberId();
        Set<Long> bookmarkedIds = bookmarkMapper.findArticleIdByMemberId(memberId);
        return new HashSet<>(bookmarkedIds);
    }

    /**
     * 현재 인증된 회원의 북마크 여부 확인
     */
    private boolean checkIfBookmarked(Long articleId) {
        if (!authenticatedMemberProvider.isAuthenticated()) {
            return false;
        }

        Long memberId = authenticatedMemberProvider.getCurrentMemberId();
        return bookmarkMapper.findExistingByMemberIdAndArticleId(memberId, articleId);
    }
}
