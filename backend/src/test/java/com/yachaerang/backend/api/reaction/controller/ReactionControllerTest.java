package com.yachaerang.backend.api.reaction.controller;

import com.yachaerang.backend.api.common.ReactionType;
import com.yachaerang.backend.api.reaction.dto.response.ReactionResponseDto;
import com.yachaerang.backend.api.reaction.service.ReactionService;
import com.yachaerang.backend.global.response.ResponseWrappingAdvice;
import com.yachaerang.backend.global.util.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;

@Import(ResponseWrappingAdvice.class)
class ReactionControllerTest extends RestDocsSupport {

    private final ReactionService reactionService = mock(ReactionService.class);

    @Override
    protected Object initController() {
        return new ReactionController(reactionService);
    }

    @Override
    protected Object[] initControllerAdvices() {
        return new Object[]{new ResponseWrappingAdvice()};
    }

    private static final String AUTH_HEADER = "Authorization";
    private static final String TEST_TOKEN = "Bearer test-access-token";
    private static final Long TEST_ARTICLE_ID = 100L;
    private static final String TEST_REACTION_TYPE = "HELPFUL";

    // 공통 응답 필드
    private static final FieldDescriptor[] ENVELOPE_COMMON = new FieldDescriptor[]{
            fieldWithPath("httpStatus").type(STRING).description("HTTP 상태 코드"),
            fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
            fieldWithPath("code").type(STRING).description("응답 코드"),
            fieldWithPath("message").type(STRING).description("응답 메시지")
    };

    private static final FieldDescriptor DATA_ARRAY_DESCRIPTOR =
            fieldWithPath("data").type(ARRAY).description("응답 데이터");

    private static final FieldDescriptor DATA_NULL_DESCRIPTOR =
            fieldWithPath("data").type(NULL).description("응답 데이터 (없음)");

    @Test
    @DisplayName("[POST] /api/v1/articles/reactions")
    void addReaction() throws Exception {
        // given
        doNothing().when(reactionService).addReaction(TEST_ARTICLE_ID, TEST_REACTION_TYPE);

        // when & then
        mockMvc.perform(post("/api/v1/articles/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN)
                        .queryParam("articleId", String.valueOf(TEST_ARTICLE_ID))
                        .queryParam("reactionType", TEST_REACTION_TYPE)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(doc(
                        "reaction-add",
                        requestHeaders(
                                headerWithName("Authorization").description("Access Token")
                        ),
                        pathParameters(),
                        queryParameters(
                                parameterWithName("articleId").description("게시글 ID"),
                                parameterWithName("reactionType").description("리액션 타입 (HELPFUL, GOOD, SURPRISED, SAD, BUMMER)")
                        ),
                        responseFields(ENVELOPE_COMMON).and(DATA_NULL_DESCRIPTOR)
                ));

        verify(reactionService).addReaction(TEST_ARTICLE_ID, TEST_REACTION_TYPE);
    }

    @Test
    @DisplayName("[DELETE] /api/v1/articles/reactions")
    void deleteReaction() throws Exception {
        // given
        doNothing().when(reactionService).removeReaction(TEST_ARTICLE_ID, TEST_REACTION_TYPE);

        // when & then
        mockMvc.perform(delete("/api/v1/articles/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN)
                        .queryParam("articleId", String.valueOf(TEST_ARTICLE_ID))
                        .queryParam("reactionType", TEST_REACTION_TYPE)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(doc(
                        "reaction-delete",
                        requestHeaders(
                                headerWithName("Authorization").description("Access Token")
                        ),
                        pathParameters(),
                        queryParameters(
                                parameterWithName("articleId").description("게시글 ID"),
                                parameterWithName("reactionType").description("삭제할 리액션 타입")
                        ),
                        responseFields(ENVELOPE_COMMON).and(DATA_NULL_DESCRIPTOR)
                ));

        verify(reactionService).removeReaction(TEST_ARTICLE_ID, TEST_REACTION_TYPE);
    }

    @Test
    @DisplayName("[GET] /api/v1/articles/reactions/{articleId}")
    void getReactionStatistics() throws Exception {
        // given
        List<ReactionResponseDto.CountDto> statistics = Arrays.asList(
                ReactionResponseDto.CountDto.builder()
                        .reactionType(ReactionType.HELPFUL)
                        .count(15L)
                        .build(),
                ReactionResponseDto.CountDto.builder()
                        .reactionType(ReactionType.GOOD)
                        .count(8L)
                        .build(),
                ReactionResponseDto.CountDto.builder()
                        .reactionType(ReactionType.SURPRISED)
                        .count(3L)
                        .build(),
                ReactionResponseDto.CountDto.builder()
                        .reactionType(ReactionType.BUMMER)
                        .count(0L)
                        .build(),
                ReactionResponseDto.CountDto.builder()
                        .reactionType(ReactionType.SAD)
                        .count(0L)
                        .build());

        when(reactionService.getReactionStatistics(TEST_ARTICLE_ID)).thenReturn(statistics);

        // when & then
        mockMvc.perform(get("/api/v1/articles/reactions/{articleId}", TEST_ARTICLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andDo(doc(
                        "reaction-getStatistics",
                        requestHeaders(
                                headerWithName("Authorization").description("Access Token")
                        ),
                        pathParameters(
                                parameterWithName("articleId").description("게시글 ID")
                        ),
                        queryParameters(),
                        responseFields(ENVELOPE_COMMON).and(DATA_ARRAY_DESCRIPTOR)
                                .andWithPrefix("data[].",
                                        fieldWithPath("reactionType").type(STRING).description("리액션 타입 (HELPFUL, GOOD, SURPRISED, SAD, BUMMER)"),
                                        fieldWithPath("count").type(NUMBER).description("해당 리액션의 개수")
                                )
                ));

        verify(reactionService).getReactionStatistics(TEST_ARTICLE_ID);
    }

    @Test
    @DisplayName("[GET] /api/v1/articles/reactions/{articleId} - 리액션 통계 조회 (빈 결과)")
    void getReactionStatistics_빈_결과() throws Exception {
        // given
        when(reactionService.getReactionStatistics(TEST_ARTICLE_ID))
                .thenReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/v1/articles/reactions/{articleId}", TEST_ARTICLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(reactionService).getReactionStatistics(TEST_ARTICLE_ID);
    }

    @Test
    @DisplayName("[GET] /api/v1/articles/reactions/{articleId}/{reactionType}")
    void getMemberReactions() throws Exception {
        // given
        List<ReactionResponseDto.MemberDto> members = Arrays.asList(
                ReactionResponseDto.MemberDto.builder()
                        .nickname("사용자1")
                        .imageUrl("https://example.com/profile1.jpg")
                        .build(),
                ReactionResponseDto.MemberDto.builder()
                        .nickname("사용자2")
                        .imageUrl("https://example.com/profile2.jpg")
                        .build(),
                ReactionResponseDto.MemberDto.builder()
                        .nickname("사용자3")
                        .imageUrl("https://example.com/profile3.jpg")
                        .build()
        );

        when(reactionService.getMemberReactions(TEST_ARTICLE_ID, TEST_REACTION_TYPE))
                .thenReturn(members);

        // when & then
        mockMvc.perform(get("/api/v1/articles/reactions/{articleId}/{reactionType}",
                        TEST_ARTICLE_ID, TEST_REACTION_TYPE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andDo(doc(
                        "reaction-getMembers",
                        requestHeaders(),
                        pathParameters(
                                parameterWithName("articleId").description("게시글 ID"),
                                parameterWithName("reactionType").description("조회할 리액션 타입 (HELPFUL, GOOD, SURPRISED, SAD, BUMMER)")
                        ),
                        queryParameters(),
                        responseFields(ENVELOPE_COMMON).and(DATA_ARRAY_DESCRIPTOR)
                                .andWithPrefix("data[].",
                                        fieldWithPath("nickname").type(STRING).description("사용자 닉네임"),
                                        fieldWithPath("imageUrl").type(STRING).description("사용자 프로필 이미지 URL")
                                )
                ));

        verify(reactionService).getMemberReactions(TEST_ARTICLE_ID, TEST_REACTION_TYPE);
    }

    @Test
    @DisplayName("[GET] /api/v1/articles/reactions/{articleId}/{reactionType} - 특정 리액션을 단 사용자 목록 조회 (빈 결과)")
    void getMemberReactions_빈_결과() throws Exception {
        // given
        when(reactionService.getMemberReactions(TEST_ARTICLE_ID, TEST_REACTION_TYPE))
                .thenReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/v1/articles/reactions/{articleId}/{reactionType}",
                        TEST_ARTICLE_ID, TEST_REACTION_TYPE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(reactionService).getMemberReactions(TEST_ARTICLE_ID, TEST_REACTION_TYPE);
    }

    @Test
    @DisplayName("[POST] /api/v1/articles/reactions - 다양한 리액션 타입 테스트")
    void addReaction_다양한_타입() throws Exception {
        // given
        String[] reactionTypes = {"HELPFUL", "GOOD", "SURPRISED", "SAD", "BUMMER"};

        for (String reactionType : reactionTypes) {
            doNothing().when(reactionService).addReaction(TEST_ARTICLE_ID, reactionType);

            // when & then
            mockMvc.perform(post("/api/v1/articles/reactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(AUTH_HEADER, TEST_TOKEN)
                            .queryParam("articleId", String.valueOf(TEST_ARTICLE_ID))
                            .queryParam("reactionType", reactionType)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        verify(reactionService, times(5)).addReaction(eq(TEST_ARTICLE_ID), anyString());
    }

    @Test
    @DisplayName("[GET] /api/v1/articles/reactions/{articleId}/{reactionType} - Authorization 헤더 없이 조회 가능")
    void getMemberReactions_Authorization_없음() throws Exception {
        // given
        List<ReactionResponseDto.MemberDto> members = Arrays.asList(
                ReactionResponseDto.MemberDto.builder()
                        .nickname("사용자1")
                        .imageUrl("https://example.com/profile1.jpg")
                        .build()
        );

        when(reactionService.getMemberReactions(TEST_ARTICLE_ID, TEST_REACTION_TYPE))
                .thenReturn(members);

        // when & then
        mockMvc.perform(get("/api/v1/articles/reactions/{articleId}/{reactionType}",
                        TEST_ARTICLE_ID, TEST_REACTION_TYPE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(reactionService).getMemberReactions(TEST_ARTICLE_ID, TEST_REACTION_TYPE);
    }
}