package com.yachaerang.backend.api.reaction.repository;

import com.yachaerang.backend.api.common.ReactionType;
import com.yachaerang.backend.api.reaction.dto.response.ReactionResponseDto;
import com.yachaerang.backend.api.reaction.entity.Reaction;
import com.yachaerang.backend.api.reaction.vo.MemberReaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReactionMapper {

    /**
     * 저장
     */
    int save(Reaction reaction);

    /**
     * memberId, articleId, reactionType으로 조회
     */
    Reaction findByMemberIdAndArticleIdAndReactionType(
            @Param("memberId") Long memberId,
            @Param("articleId") Long articleId,
            @Param("reactionType") String reactionType
    );

    /**
     * memberId와 articleId로 리액션 삭제
     */
    int delete(
            @Param("memberId") Long memberId,
            @Param("articleId") Long articleId,
            @Param("reactionType") String reactionType
    );

    /**
     * 특정 게시글의 특정 리액션 타입을 단 사용자들 조회
     */
    List<MemberReaction> findMembersByArticleIdAndReactionType(
            @Param("articleId") Long articleId,
            @Param("reactionType") ReactionType reactionType
    );

    /**
     * 특정 게시글의 리액션 통계 조회
     */
    List<ReactionResponseDto.CountDto> countByArticleId(@Param("articleId") Long articleId);
}
