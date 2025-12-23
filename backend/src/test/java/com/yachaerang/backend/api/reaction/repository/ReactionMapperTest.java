package com.yachaerang.backend.api.reaction.repository;

import com.yachaerang.backend.api.common.MemberStatus;
import com.yachaerang.backend.api.common.ReactionType;
import com.yachaerang.backend.api.member.entity.Member;
import com.yachaerang.backend.api.member.repository.MemberMapper;
import com.yachaerang.backend.api.reaction.dto.response.ReactionResponseDto;
import com.yachaerang.backend.api.reaction.entity.Reaction;
import com.yachaerang.backend.api.reaction.vo.MemberReaction;
import com.yachaerang.backend.global.config.MyBatisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@MybatisTest
@Import(MyBatisConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("classpath:H2_schema.sql")
class ReactionMapperTest {

    @Autowired private ReactionMapper reactionMapper;

    @Autowired private MemberMapper memberMapper;

    private Long testMemberId;
    private Long testArticleId;

    @BeforeEach
    void setUp() {
        Member member = Member.builder()
                .email("test@example.com")
                .name("테스터")
                .nickname("테스터")
                .password("password123")
                .memberCode("testCode")
                .imageUrl("default.png")
                .memberStatus(MemberStatus.ACTIVE)
                .build();
        memberMapper.save(member);
        testMemberId = member.getId();
        testArticleId = 100L;
    }

    @Test
    @DisplayName("리액션 저장 성공")
    void 리액션_저장_성공() {
        // given
        Reaction reaction = Reaction.builder()
                .memberId(testMemberId)
                .articleId(testArticleId)
                .reactionType(ReactionType.HELPFUL)
                .build();

        // when
        int result = reactionMapper.save(reaction);

        // then
        assertThat(result).isEqualTo(1);
        assertThat(reaction.getReactionId()).isNotNull();
    }

    @Test
    @DisplayName("memberId, articleId, reactionType으로 리액션 조회 성공")
    void findByMemberIdAndArticleIdAndReactionType_성공() {
        // given
        Reaction reaction = Reaction.builder()
                .memberId(testMemberId)
                .articleId(testArticleId)
                .reactionType(ReactionType.HELPFUL)
                .build();
        reactionMapper.save(reaction);

        // when
        Reaction found = reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, ReactionType.HELPFUL.getCode());

        // then
        assertThat(found).isNotNull();
        assertThat(found.getMemberId()).isEqualTo(testMemberId);
        assertThat(found.getArticleId()).isEqualTo(testArticleId);
        assertThat(found.getReactionType()).isEqualTo(ReactionType.HELPFUL);
    }

    @Test
    @DisplayName("존재하지 않는 리액션 조회 시 null 반환")
    void 존재하지_않는_리액션_조회시_null_반환() {
        // when
        Reaction found = reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, ReactionType.HELPFUL.getCode());

        // then
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("리액션 삭제 성공")
    void 리액션_삭제_성공() {
        // given
        Reaction reaction = Reaction.builder()
                .memberId(testMemberId)
                .articleId(testArticleId)
                .reactionType(ReactionType.GOOD)
                .build();
        reactionMapper.save(reaction);

        // when
        int result = reactionMapper.delete(testMemberId, testArticleId, ReactionType.GOOD.getCode());

        // then
        assertThat(result).isEqualTo(1);

        Reaction found = reactionMapper.findByMemberIdAndArticleIdAndReactionType(
                testMemberId, testArticleId, ReactionType.GOOD.getCode());
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 리액션 삭제 시 0 반환")
    void delete_없음() {
        // when
        int result = reactionMapper.delete(testMemberId, testArticleId, ReactionType.HELPFUL.getCode());

        // then
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("특정 게시글의 특정 리액션을 단 사용자들 조회 성공")
    void 특정게시글의_특정리액션을_단_사용자들_조회_성공() {
        // given
        Member member2 = Member.builder()
                .email("test2@example.com")
                .name("테스터2")
                .nickname("테스터2")
                .password("password123")
                .memberCode("testCode2")
                .imageUrl("default.png")
                .memberStatus(MemberStatus.ACTIVE)
                .build();
        memberMapper.save(member2);

        Reaction reaction1 = Reaction.builder()
                .memberId(testMemberId)
                .articleId(testArticleId)
                .reactionType(ReactionType.HELPFUL)
                .build();
        reactionMapper.save(reaction1);

        Reaction reaction2 = Reaction.builder()
                .memberId(member2.getId())
                .articleId(testArticleId)
                .reactionType(ReactionType.HELPFUL)
                .build();
        reactionMapper.save(reaction2);

        // when
        List<MemberReaction> memberReactions = reactionMapper.findMembersByArticleIdAndReactionType(
                testArticleId, ReactionType.HELPFUL);

        // then
        assertThat(memberReactions).hasSize(2);
        assertThat(memberReactions)
                .extracting(MemberReaction::getMemberId)
                .containsExactlyInAnyOrder(testMemberId, member2.getId());
        assertThat(memberReactions)
                .allMatch(mr -> mr.getReactionType() == ReactionType.HELPFUL);
    }

    @Test
    @DisplayName("특정 게시글의 리액션 통계 조회 성공")
    void 특정_게시글의_리액션_통계_조회_성공() {
        // given
        Member member2 = Member.builder()
                .email("test2@example.com")
                .name("테스터2")
                .nickname("테스터2")
                .memberCode("testCode2")
                .password("password123")
                .memberStatus(MemberStatus.ACTIVE)
                .build();
        memberMapper.save(member2);

        Member member3 = Member.builder()
                .email("test3@example.com")
                .name("테스터3")
                .nickname("테스터3")
                .memberCode("testCode3")
                .password("password123")
                .memberStatus(MemberStatus.ACTIVE)
                .build();
        memberMapper.save(member3);

        // HELPFUL 2개
        reactionMapper.save(Reaction.builder()
                .memberId(testMemberId)
                .articleId(testArticleId)
                .reactionType(ReactionType.HELPFUL)
                .build());
        reactionMapper.save(Reaction.builder()
                .memberId(member2.getId())
                .articleId(testArticleId)
                .reactionType(ReactionType.HELPFUL)
                .build());

        // GOOD 1개
        reactionMapper.save(Reaction.builder()
                .memberId(member3.getId())
                .articleId(testArticleId)
                .reactionType(ReactionType.GOOD)
                .build());

        // when
        List<ReactionResponseDto.CountDto> statistics = reactionMapper.countByArticleId(testArticleId);

        // then
        assertThat(statistics).hasSize(2);
        assertThat(statistics)
                .extracting(ReactionResponseDto.CountDto::getReactionType,
                        ReactionResponseDto.CountDto::getCount)
                .containsExactlyInAnyOrder(
                        tuple(ReactionType.HELPFUL, 2L),
                        tuple(ReactionType.GOOD, 1L)
                );
    }

    @Test
    @DisplayName("리액션이 없는 게시글의 통계 조회 시 빈 리스트 반환")
    void 리액션이_없으면_빈리스트_반환() {
        // when
        List<ReactionResponseDto.CountDto> statistics = reactionMapper.countByArticleId(testArticleId);

        // then
        assertThat(statistics).isEmpty();
    }
}