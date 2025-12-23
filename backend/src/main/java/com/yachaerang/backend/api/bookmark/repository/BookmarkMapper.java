package com.yachaerang.backend.api.bookmark.repository;

import com.yachaerang.backend.api.bookmark.vo.ArticleBookmark;
import com.yachaerang.backend.api.bookmark.entity.Bookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookmarkMapper {

    /**
     * 북마크 등록하기
     */
    int save(Bookmark bookmark);

    /**
     * 북마크 해제하기
     */
    int delete(@Param("memberId") Long memberId, @Param("articleId") Long articleId);

    /**
     * memberId와 articleId를 기반으로 조회하기
     * @param memberId
     * @param articleId
     * @return
     */
    Bookmark findByMemberIdAndArticleId(
            @Param("memberId") Long memberId,
            @Param("articleId") Long articleId);

    /**
     * memberId를 기반으로 전체 Bookmark 조회
     * @param memberId
     * @return
     */
    List<ArticleBookmark> findAllByMemberId(@Param("memberId") Long memberId);
}
