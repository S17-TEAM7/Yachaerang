package com.yachaerang.backend.api.bookmark.controller;

import com.yachaerang.backend.api.bookmark.dto.response.BookmarkResponseDto;
import com.yachaerang.backend.api.bookmark.service.BookmarkService;
import com.yachaerang.backend.global.response.ResponseWrappingAdvice;
import com.yachaerang.backend.global.util.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;

@Import(ResponseWrappingAdvice.class)
class BookmarkControllerTest extends RestDocsSupport {

    private final BookmarkService bookmarkService = mock(BookmarkService.class);

    @Override
    protected Object initController() {
        return new BookmarkController(bookmarkService);
    }

    @Override
    protected Object[] initControllerAdvices() {
        return new Object[]{new ResponseWrappingAdvice()};
    }

    private static final String AUTH_HEADER = "Authorization";
    private static final String TEST_TOKEN = "Bearer test-access-token";
    private static final Long TEST_ARTICLE_ID = 100L;

    // 공통 응답 필드
    private static final FieldDescriptor[] ENVELOPE_COMMON = new FieldDescriptor[]{
            fieldWithPath("httpStatus").type(STRING).description("HTTP 상태 코드"),
            fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
            fieldWithPath("code").type(STRING).description("응답 코드"),
            fieldWithPath("message").type(STRING).description("응답 메시지")
    };

    /**
     * data 가 존재하는 경우의 필드
     */
    private static final FieldDescriptor DATA_ARRAY_DESCRIPTOR =
            fieldWithPath("data").type(ARRAY).description("응답 데이터");

    /**
     * data 가 존재하지 않는 경우(null) 의 필드
     */
    private static final FieldDescriptor DATA_NULL_DESCRIPTOR =
            fieldWithPath("data").type(NULL).description("응답 데이터 (없음)");


    @Test
    @DisplayName("[POST] /api/v1/bookmarks")
    void register() throws Exception {
        // given
        doNothing().when(bookmarkService).registerBookmark(TEST_ARTICLE_ID);

        // when & then
        mockMvc.perform(post("/api/v1/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN)
                        .queryParam("articleId", String.valueOf(TEST_ARTICLE_ID))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(doc(
                        "bookmark-save",
                        requestHeaders(
                                headerWithName("Authorization").description("Access Token")
                        ),
                        pathParameters(),
                        queryParameters(
                                parameterWithName("articleId").description("북마크할 게시글 ID")
                        ),
                        responseFields(ENVELOPE_COMMON).and(DATA_NULL_DESCRIPTOR)
                ));

        verify(bookmarkService).registerBookmark(TEST_ARTICLE_ID);
    }

    @Test
    @DisplayName("[DELETE] /api/v1/bookmarks")
    void deleteBookmarks() throws Exception {
        // given
        doNothing().when(bookmarkService).eraseBookmark(TEST_ARTICLE_ID);

        // when & then
        mockMvc.perform(delete("/api/v1/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN)
                        .queryParam("articleId", String.valueOf(TEST_ARTICLE_ID))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(doc(
                        "bookmark-delete",
                        requestHeaders(
                                headerWithName("Authorization").description("Access Token")
                        ),
                        pathParameters(),
                        queryParameters(
                                parameterWithName("articleId").description("삭제할 북마크의 게시글 ID")
                        ),
                        responseFields(ENVELOPE_COMMON).and(DATA_NULL_DESCRIPTOR)
                ));
        verify(bookmarkService).eraseBookmark(TEST_ARTICLE_ID);
    }


    @Test
    @DisplayName("[GET] /api/v1/bookmarks")
    void getAll() throws Exception {
        // given
        BookmarkResponseDto.GetAllDto dto1 = BookmarkResponseDto.GetAllDto.builder()
                .bookmarkId(1L)
                .articleId(100L)
                .title("첫 번째 게시글")
                .tagList(Arrays.asList("Spring", "Java"))
                .build();
        BookmarkResponseDto.GetAllDto dto2 = BookmarkResponseDto.GetAllDto.builder()
                .bookmarkId(2L)
                .articleId(200L)
                .title("두 번째 게시글")
                .tagList(Arrays.asList("Python", "Java"))
                .build();
        List<BookmarkResponseDto.GetAllDto> dtoList = List.of(dto1, dto2);
        when(bookmarkService.getAll()).thenReturn(dtoList);

        // when & then
        mockMvc.perform(get("/api/v1/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTH_HEADER, TEST_TOKEN)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(doc(
                        "bookmark-getAll",
                        requestHeaders(
                                headerWithName("Authorization").description("Access Token")
                        ),
                        // pathParameters() 제거 - path variable이 없으므로
                        // queryParameters() 제거 - query parameter가 없으므로
                        responseFields(ENVELOPE_COMMON).and(DATA_ARRAY_DESCRIPTOR)
                                .andWithPrefix("data[].",
                                        fieldWithPath("bookmarkId").type(NUMBER).description("북마크 ID"),
                                        fieldWithPath("articleId").type(NUMBER).description("게시글 ID"),
                                        fieldWithPath("title").type(STRING).description("게시글 제목"),
                                        fieldWithPath("tagList").type(ARRAY).description("태그 이름 리스트")
                                )
                ));

        verify(bookmarkService).getAll();
    }
}