package com.yachaerang.backend.api.reaction.service;

import com.yachaerang.backend.api.common.ReactionType;
import com.yachaerang.backend.api.member.entity.Member;
import com.yachaerang.backend.api.reaction.dto.response.ReactionResponseDto;
import com.yachaerang.backend.api.reaction.entity.Reaction;
import com.yachaerang.backend.api.reaction.repository.ReactionMapper;
import com.yachaerang.backend.api.reaction.vo.MemberReaction;
import com.yachaerang.backend.global.auth.jwt.AuthenticatedMemberProvider;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock
    private ReactionMapper reactionMapper;

    @Mock
    private AuthenticatedMemberProvider authenticatedMemberProvider;

    @InjectMocks
    private ReactionService reactionService;

    private Long testMemberId;
    private Long testArticleId;
    private String testReactionType;

    @BeforeEach
    void setUp() {
        testMemberId = 1L;
        testArticleId = 100L;
        testReactionType = "HELPFUL";
    }

    @Test
    @DisplayName("리액션 추가 성공")
    void addReaction_성공() {
        // given
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, testReactionType))
                .thenReturn(null);
        when(reactionMapper.save(any(Reaction.class))).thenReturn(1);

        // when
        reactionService.addReaction(testArticleId, testReactionType);

        // then
        verify(authenticatedMemberProvider).getCurrentMemberId();
        verify(reactionMapper).findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, testReactionType);
        verify(reactionMapper).save(any(Reaction.class));
    }

    @Test
    @DisplayName("리액션 추가 실패 - 이미 동일 리액션 존재")
    void addReaction_실패_이미_존재() {
        // given
        Reaction existingReaction = Reaction.builder()
                .reactionId(1L)
                .memberId(testMemberId)
                .articleId(testArticleId)
                .reactionType(ReactionType.HELPFUL)
                .build();

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, testReactionType))
                .thenReturn(existingReaction);

        // when & then
        assertThatThrownBy(() -> reactionService.addReaction(testArticleId, testReactionType))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REACTION_ALREADY);

        verify(reactionMapper, never()).save(any(Reaction.class));
    }

    @Test
    @DisplayName("리액션 추가 실패 - DB 저장 실패")
    void addReaction_실패_DB_저장_실패() {
        // given
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, testReactionType))
                .thenReturn(null);
        when(reactionMapper.save(any(Reaction.class))).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> reactionService.addReaction(testArticleId, testReactionType))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REACTION_DB_FAILED);
    }

    @Test
    @DisplayName("리액션 추가 실패 - 잘못된 reactionType")
    void addReaction_실패_잘못된_타입() {
        // given
        String invalidReactionType = "INVALID_TYPE";
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, invalidReactionType))
                .thenReturn(null);

        // when & then
        assertThatThrownBy(() -> reactionService.addReaction(testArticleId, invalidReactionType))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENUM_MAPPED_FAILED);

        verify(reactionMapper, never()).save(any(Reaction.class));
    }

    @Test
    @DisplayName("리액션 삭제 성공")
    void removeReaction_성공() {
        // given
        Reaction existingReaction = Reaction.builder()
                .reactionId(1L)
                .memberId(testMemberId)
                .articleId(testArticleId)
                .reactionType(ReactionType.HELPFUL)
                .build();

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, testReactionType))
                .thenReturn(existingReaction);
        when(reactionMapper.delete(testMemberId, testArticleId, testReactionType))
                .thenReturn(1);

        // when
        reactionService.removeReaction(testArticleId, testReactionType);

        // then
        verify(authenticatedMemberProvider).getCurrentMemberId();
        verify(reactionMapper).findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, testReactionType);
        verify(reactionMapper).delete(testMemberId, testArticleId, testReactionType);
    }

    @Test
    @DisplayName("리액션 삭제 실패 - 리액션이 존재하지 않음")
    void removeReaction_실패_존재하지_않음() {
        // given
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, testReactionType))
                .thenReturn(null);

        // when & then
        assertThatThrownBy(() -> reactionService.removeReaction(testArticleId, testReactionType))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REACTION_NOT_FOUND);

        verify(reactionMapper, never()).delete(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("리액션 삭제 실패 - DB 삭제 실패")
    void removeReaction_실패_DB_삭제_실패() {
        // given
        Reaction existingReaction = Reaction.builder()
                .reactionId(1L)
                .memberId(testMemberId)
                .articleId(testArticleId)
                .reactionType(ReactionType.HELPFUL)
                .build();

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, testReactionType))
                .thenReturn(existingReaction);
        when(reactionMapper.delete(testMemberId, testArticleId, testReactionType))
                .thenReturn(0);

        // when & then
        assertThatThrownBy(() -> reactionService.removeReaction(testArticleId, testReactionType))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REACTION_DB_FAILED);
    }

    @Test
    @DisplayName("특정 리액션을 단 사용자 목록 조회 성공")
    void getMemberReactions_성공() {
        // given
        Member member1 = new Member();
        member1.setId(1L);
        member1.setNickname("사용자1");
        member1.setImageUrl("image1.png");

        Member member2 = new Member();
        member2.setId(2L);
        member2.setNickname("사용자2");
        member2.setImageUrl("image2.png");

        MemberReaction memberReaction1 = new MemberReaction();
        memberReaction1.setReactionId(1L);
        memberReaction1.setMemberId(1L);
        memberReaction1.setArticleId(testArticleId);
        memberReaction1.setReactionType(ReactionType.HELPFUL);
        memberReaction1.setMember(member1);

        MemberReaction memberReaction2 = new MemberReaction();
        memberReaction2.setReactionId(2L);
        memberReaction2.setMemberId(2L);
        memberReaction2.setArticleId(testArticleId);
        memberReaction2.setReactionType(ReactionType.HELPFUL);
        memberReaction2.setMember(member2);

        when(reactionMapper.findMembersByArticleIdAndReactionType(
                testArticleId, ReactionType.HELPFUL))
                .thenReturn(Arrays.asList(memberReaction1, memberReaction2));

        // when
        List<ReactionResponseDto.MemberDto> result =
                reactionService.getMemberReactions(testArticleId, testReactionType);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNickname()).isEqualTo("사용자1");
        assertThat(result.get(0).getImageUrl()).isEqualTo("image1.png");
        assertThat(result.get(1).getNickname()).isEqualTo("사용자2");
        assertThat(result.get(1).getImageUrl()).isEqualTo("image2.png");

        verify(reactionMapper).findMembersByArticleIdAndReactionType(
                testArticleId, ReactionType.HELPFUL);
    }

    @Test
    @DisplayName("특정 리액션을 단 사용자가 없는 경우 빈 리스트 반환")
    void getMemberReactions_빈_리스트() {
        // given
        when(reactionMapper.findMembersByArticleIdAndReactionType(
                testArticleId, ReactionType.HELPFUL))
                .thenReturn(Collections.emptyList());

        // when
        List<ReactionResponseDto.MemberDto> result =
                reactionService.getMemberReactions(testArticleId, testReactionType);

        // then
        assertThat(result).isEmpty();
        verify(reactionMapper).findMembersByArticleIdAndReactionType(
                testArticleId, ReactionType.HELPFUL);
    }

    @Test
    @DisplayName("특정 리액션을 단 사용자 목록 조회 실패 - 잘못된 reactionType")
    void getMemberReactions_실패_잘못된_타입() {
        // given
        String invalidReactionType = "INVALID_TYPE";

        // when & then
        assertThatThrownBy(() -> reactionService.getMemberReactions(testArticleId, invalidReactionType))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENUM_MAPPED_FAILED);

        verify(reactionMapper, never()).findMembersByArticleIdAndReactionType(anyLong(), any());
    }

    @Test
    @DisplayName("리액션 통계 조회 성공")
    void getReactionStatistics_성공() {
        // given
        List<ReactionResponseDto.CountDto> mockStats = Arrays.asList(
                ReactionResponseDto.CountDto.builder()
                        .reactionType(ReactionType.HELPFUL)
                        .count(5L)
                        .build(),
                ReactionResponseDto.CountDto.builder()
                        .reactionType(ReactionType.GOOD)
                        .count(3L)
                        .build(),
                ReactionResponseDto.CountDto.builder()
                        .reactionType(ReactionType.SURPRISED)
                        .count(2L)
                        .build()
        );

        when(reactionMapper.countByArticleId(testArticleId)).thenReturn(mockStats);

        // when
        List<ReactionResponseDto.CountDto> result =
                reactionService.getReactionStatistics(testArticleId);

        // then
        assertThat(result).hasSize(ReactionType.values().length);

        assertThat(result).extracting(
                        ReactionResponseDto.CountDto::getReactionType,
                        ReactionResponseDto.CountDto::getCount
                )
                .containsExactlyInAnyOrder(
                        tuple(ReactionType.HELPFUL, 5L),
                        tuple(ReactionType.GOOD, 3L),
                        tuple(ReactionType.SAD, 0L),
                        tuple(ReactionType.BUMMER, 0L),
                        tuple(ReactionType.SURPRISED, 2L)
                );

        verify(reactionMapper).countByArticleId(testArticleId);
    }

    @Test
    @DisplayName("리액션이 없는 게시글의 통계 조회 시 빈 리스트 반환")
    void getReactionStatistics_빈_리스트() {
        // given
        when(reactionMapper.countByArticleId(testArticleId))
                .thenReturn(Collections.emptyList());

        // when
        List<ReactionResponseDto.CountDto> result =
                reactionService.getReactionStatistics(testArticleId);

        // then
        assertThat(result).hasSize(ReactionType.values().length);

        assertThat(result)
                .allSatisfy(dto -> assertThat(dto.getCount()).isZero());

        assertThat(result)
                .extracting(ReactionResponseDto.CountDto::getReactionType)
                .containsExactlyInAnyOrder(ReactionType.values());

        verify(reactionMapper).countByArticleId(testArticleId);
    }

    @Test
    @DisplayName("여러 종류의 리액션 타입 추가 성공")
    void addReaction_여러_타입_성공() {
        // given
        String[] reactionTypes = {"HELPFUL", "GOOD", "SURPRISED", "SAD", "BUMMER"};

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                anyLong(), anyLong(), anyString()))
                .thenReturn(null);
        when(reactionMapper.save(any(Reaction.class))).thenReturn(1);

        // when & then
        for (String reactionType : reactionTypes) {
            assertThatCode(() -> reactionService.addReaction(testArticleId, reactionType))
                    .doesNotThrowAnyException();
        }

        verify(reactionMapper, times(5)).save(any(Reaction.class));
    }

    @Test
    @DisplayName("동일 사용자가 같은 게시글에 다른 리액션 추가 가능")
    void addReaction_다른_타입_추가_가능() {
        // given
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);

        // HELPFUL 리액션은 이미 존재
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, "HELPFUL"))
                .thenReturn(Reaction.builder().build());

        // GOOD 리액션은 없음
        when(reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, "GOOD"))
                .thenReturn(null);

        when(reactionMapper.save(any(Reaction.class))).thenReturn(1);

        // when & then
        assertThatThrownBy(() -> reactionService.addReaction(testArticleId, "HELPFUL"))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REACTION_ALREADY);

        assertThatCode(() -> reactionService.addReaction(testArticleId, "GOOD"))
                .doesNotThrowAnyException();
    }
}