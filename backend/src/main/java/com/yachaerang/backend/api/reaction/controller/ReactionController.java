package com.yachaerang.backend.api.reaction.controller;

import com.yachaerang.backend.api.reaction.dto.response.ReactionResponseDto;
import com.yachaerang.backend.api.reaction.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/articles/reactions")
@RestController
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("")
    public void addReaction(
            @RequestHeader("Authorization") String token,
            @RequestParam("articleId") Long articleId,
            @RequestParam("reactionType") String reactionType) {
        reactionService.addReaction(articleId, reactionType);
    }

    @DeleteMapping("")
    public void deleteReaction(
            @RequestHeader("Authorization") String token,
            @RequestParam("articleId") Long articleId,
            @RequestParam("reactionType") String reactionType) {
        reactionService.removeReaction(articleId, reactionType);
    }


    @GetMapping("/{articleId}")
    public List<ReactionResponseDto.CountDto> getReactionStatistics(
            @PathVariable("articleId") Long articleId
    ) {
        return reactionService.getReactionStatistics(articleId);
    }

    @GetMapping("/{articleId}/{reactionType}")
    public List<ReactionResponseDto.MemberDto> getMemberReactions(
            @PathVariable("articleId") Long articleId,
            @PathVariable("reactionType") String reactionType
    ) {
        return reactionService.getMemberReactions(articleId, reactionType);
    }
}
