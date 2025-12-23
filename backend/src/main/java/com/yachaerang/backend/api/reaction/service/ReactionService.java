package com.yachaerang.backend.api.reaction.service;

import com.yachaerang.backend.api.common.ReactionType;
import com.yachaerang.backend.api.reaction.dto.response.ReactionResponseDto;
import com.yachaerang.backend.api.reaction.entity.Reaction;
import com.yachaerang.backend.api.reaction.repository.ReactionMapper;
import com.yachaerang.backend.api.reaction.vo.MemberReaction;
import com.yachaerang.backend.global.auth.jwt.AuthenticatedMemberProvider;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionMapper reactionMapper;
    private final AuthenticatedMemberProvider authenticatedMemberProvider;

    /**
     * 리액션 달기
     */
    @Transactional
    public void addReaction(
            Long articleId, String reactionType
    ) {
        Long memberId = authenticatedMemberProvider.getCurrentMemberId();

        // 이미 동일 리액션 있으면 안 됨
        if (reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                memberId, articleId, reactionType) != null) {
            throw GeneralException.of(ErrorCode.REACTION_ALREADY);
        }

        Reaction reaction = Reaction.builder()
                .memberId(memberId)
                .articleId(articleId)
                .reactionType(ReactionType.fromCode(reactionType))
                .build();

        int result = reactionMapper.save(reaction);
        if (result != 1) {
            throw GeneralException.of(ErrorCode.REACTION_DB_FAILED);
        }
    }

    /**
     * 리액션 취소하기
     */
    @Transactional
    public void removeReaction(Long articleId, String reactionType) {
        Long memberId = authenticatedMemberProvider.getCurrentMemberId();

        // 리액션이 존재하는지 확인
        Reaction existingReaction = reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                memberId, articleId, reactionType);

        if (existingReaction == null) {
            throw GeneralException.of(ErrorCode.REACTION_NOT_FOUND);
        }

        int result = reactionMapper.delete(memberId, articleId, reactionType);

        if (result != 1) {
            throw GeneralException.of(ErrorCode.REACTION_DB_FAILED);
        }
    }


    /**
     * 특정 기사, 특정 리액션을 단 사람들 조회하기
     */
    @Transactional(readOnly = true)
    public List<ReactionResponseDto.MemberDto> getMemberReactions(
            Long articleId, String reactionType
    ) {
        List<MemberReaction> memberReactionList = reactionMapper.findMembersByArticleIdAndReactionType(articleId, ReactionType.fromCode(reactionType));

        return memberReactionList.stream()
                .map(memberReaction -> ReactionResponseDto.MemberDto.builder()
                        .nickname(memberReaction.getMember().getNickname())
                        .imageUrl(memberReaction.getMember().getImageUrl())
                        .build())
                .toList();
    }

    /**
     * 특정 게시글의 리액션 통계 조회
     */
    @Transactional(readOnly = true)
    public List<ReactionResponseDto.CountDto> getReactionStatistics(Long articleId) {

        Map<ReactionType, Long> countMap =
                reactionMapper.countByArticleId(articleId).stream()
                        .collect(Collectors.toMap(
                                ReactionResponseDto.CountDto::getReactionType,
                                ReactionResponseDto.CountDto::getCount
                        ));

        return Arrays.stream(ReactionType.values())
                .map(type -> ReactionResponseDto.CountDto.builder()
                        .reactionType(type)
                        .count(countMap.getOrDefault(type, 0L))
                        .build())
                .toList();
    }
}
