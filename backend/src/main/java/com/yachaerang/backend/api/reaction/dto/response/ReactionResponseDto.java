package com.yachaerang.backend.api.reaction.dto.response;

import com.yachaerang.backend.api.common.ReactionType;
import lombok.*;

public class ReactionResponseDto {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CountDto {
        private ReactionType reactionType;
        private Long count;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MemberDto {
        private String nickname;
        private String imageUrl;
    }
}
