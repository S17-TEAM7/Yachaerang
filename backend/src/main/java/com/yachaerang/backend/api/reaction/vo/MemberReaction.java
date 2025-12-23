package com.yachaerang.backend.api.reaction.vo;

import com.yachaerang.backend.api.common.ReactionType;
import com.yachaerang.backend.api.member.entity.Member;
import lombok.*;

/*
 Join의 결과를 담기 위한 VO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberReaction {
    private Long reactionId;
    private Long memberId;
    private Long articleId;
    private ReactionType reactionType;
    private Member member;
}
