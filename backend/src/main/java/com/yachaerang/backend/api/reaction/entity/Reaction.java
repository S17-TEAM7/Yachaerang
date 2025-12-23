package com.yachaerang.backend.api.reaction.entity;

import com.yachaerang.backend.api.common.BaseEntity;
import com.yachaerang.backend.api.common.ReactionType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Reaction extends BaseEntity {

    private Long reactionId;
    private Long memberId;
    private Long articleId;
    private ReactionType reactionType;
}
