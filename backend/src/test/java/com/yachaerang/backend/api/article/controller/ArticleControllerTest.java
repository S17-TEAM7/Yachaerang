package com.yachaerang.backend.api.article.controller;

import com.yachaerang.backend.api.article.dto.request.ArticleRequestDto;
import com.yachaerang.backend.api.article.dto.response.ArticleResponseDto;
import com.yachaerang.backend.api.article.service.ArticleService;
import com.yachaerang.backend.global.response.ResponseWrappingAdvice;
import com.yachaerang.backend.global.util.RestDocsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(ResponseWrappingAdvice.class)
class ArticleControllerTest extends RestDocsSupport {

    private final ArticleService articleService = mock(ArticleService.class);

    @Override
    protected Object initController() {
        return new ArticleController(articleService);
    }

    @Override
    protected Object[] initControllerAdvices() {
        return new Object[]{ new ResponseWrappingAdvice()};
    }

    // 공통 응답 필드
    private static final FieldDescriptor[] ENVELOPE_COMMON = new FieldDescriptor[]{
            fieldWithPath("httpStatus").type(STRING).description("HTTP 상태 코드"),
            fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
            fieldWithPath("code").type(STRING).description("응답 코드"),
            fieldWithPath("message").type(STRING).description("응답 메시지")
    };

    /**
     * Pagination에 대한 응답
     */
    private static final FieldDescriptor[] PAGE_FIELDS = new FieldDescriptor[]{
            fieldWithPath("content[]").type(ARRAY).description("콘텐츠 목록"),
            fieldWithPath("currentPage").type(NUMBER).description("현재 페이지"),
            fieldWithPath("pageSize").type(NUMBER).description("페이지 크기"),
            fieldWithPath("totalElements").type(NUMBER).description("전체 요소 수"),
            fieldWithPath("totalPages").type(NUMBER).description("전체 페이지 수"),
            fieldWithPath("first").type(BOOLEAN).description("첫 페이지 여부"),
            fieldWithPath("last").type(BOOLEAN).description("마지막 페이지 여부"),
            fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
            fieldWithPath("hasPrevious").type(BOOLEAN).description("이전 페이지 존재 여부")
    };


    private ArticleResponseDto.ListDto listDto1;
    private ArticleResponseDto.ListDto listDto2;
    private ArticleResponseDto.DetailDto detailDto;

    @BeforeEach
    void setUp() {
        listDto1 = ArticleResponseDto.ListDto.builder()
                .articleId(1L)
                .title("제목1")
                .imageUrl("default.png")
                .createdAt(LocalDate.of(2025, 12, 6))
                .tagList(List.of("태그1", "태그2"))
                .isBookmarked(false)
                .build();

        listDto2 = ArticleResponseDto.ListDto.builder()
                .articleId(2L)
                .title("제목2")
                .imageUrl("default.png")
                .createdAt(LocalDate.of(2025, 12, 5))
                .tagList(List.of("태그3"))
                .isBookmarked(false)
                .build();

        detailDto = ArticleResponseDto.DetailDto.builder()
                .articleId(1L)
                .title("제목1")
                .content("내용1")
                .imageUrl("default.png")
                .url("http://example.com/1")
                .tagList(List.of("태그1", "태그2"))
                .createdAt(LocalDate.of(2025, 12, 6))
                .isBookmarked(false)
                .build();
    }

    @Test
    @DisplayName("[GET] /api/v1/articles")
    void getAllArticles() throws Exception {
        // given
        ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> pageDto =
                ArticleResponseDto.PageDto.<ArticleResponseDto.ListDto>builder()
                        .content(List.of(listDto1, listDto2))
                        .currentPage(1)
                        .pageSize(5)
                        .totalElements(2L)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build();

        given(articleService.getAllArticles(any(ArticleRequestDto.PageDto.class)))
                .willReturn(pageDto);

        // when & then
        mockMvc.perform(get("/api/v1/articles")
                        .param("page", "1")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(doc(
                        "get-all-articles",
                        requestHeaders(),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)"),
                                parameterWithName("size").description("페이지 크기 (기본값: 5)")
                        ),
                        responseFields(ENVELOPE_COMMON)
                                .and(fieldWithPath("data").type(OBJECT).description("응답 데이터"))
                                .andWithPrefix("data.", PAGE_FIELDS)
                                .andWithPrefix("data.content[].",
                                        fieldWithPath("articleId").type(NUMBER).description("기사 ID"),
                                        fieldWithPath("title").type(STRING).description("기사 제목"),
                                        fieldWithPath("imageUrl").type(STRING).description("이미지 URL"),
                                        fieldWithPath("tagList[]").type(ARRAY).description("태그 목록"),
                                        fieldWithPath("createdAt").type(STRING).description("작성일"),
                                        fieldWithPath("isBookmarked").type(BOOLEAN).description("북마크 여부")
                                )
                ));
    }

    @Test
    @DisplayName("[POST] /api/v1/articles/{articleId}")
    void getArticle() throws Exception {
        // given
        Long articleId = 1L;
        given(articleService.getArticle(articleId)).willReturn(detailDto);

        // when & then
        mockMvc.perform(get("/api/v1/articles/{articleId}", articleId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(doc(
                        "get-article",
                        requestHeaders(),
                        pathParameters(
                                parameterWithName("articleId").description("기사 ID")
                        ),
                        responseFields(ENVELOPE_COMMON)
                                .and(fieldWithPath("data").type(OBJECT).description("응답 데이터"))
                                .andWithPrefix("data.",
                                        fieldWithPath("articleId").type(NUMBER).description("기사 ID"),
                                        fieldWithPath("title").type(STRING).description("기사 제목"),
                                        fieldWithPath("content").type(STRING).description("기사 내용"),
                                        fieldWithPath("imageUrl").type(STRING).description("이미지 URL"),
                                        fieldWithPath("url").type(STRING).description("원본 URL"),
                                        fieldWithPath("tagList[]").type(ARRAY).description("태그 목록"),
                                        fieldWithPath("createdAt").type(STRING).description("작성일"),
                                        fieldWithPath("isBookmarked").type(BOOLEAN).description("북마크 여부")
                                )
                ));
    }

    @Test
    @DisplayName("[GET] /api/v1/articles/search")
    void searchArticles() throws Exception {
        // given
        String keyword = "제목1";
        ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> pageDto =
                ArticleResponseDto.PageDto.<ArticleResponseDto.ListDto>builder()
                        .content(List.of(listDto1, listDto2))
                        .currentPage(1)
                        .pageSize(5)
                        .totalElements(2L)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build();

        given(articleService.searchArticles(any(ArticleRequestDto.PageDto.class), any(String.class)))
                .willReturn(pageDto);

        // when & then
        mockMvc.perform(get("/api/v1/articles/search")
                        .param("page", "1")
                        .param("size", "5")
                        .param("keyword", keyword)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(doc(
                        "search-articles",
                        requestHeaders(),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)"),
                                parameterWithName("size").description("페이지 크기 (기본값: 5)"),
                                parameterWithName("keyword").description("검색할 키워드")
                        ),
                        responseFields(ENVELOPE_COMMON)
                                .and(fieldWithPath("data").type(OBJECT).description("응답 데이터"))
                                .andWithPrefix("data.", PAGE_FIELDS)
                                .andWithPrefix("data.content[].",
                                        fieldWithPath("articleId").type(NUMBER).description("기사 ID"),
                                        fieldWithPath("title").type(STRING).description("기사 제목"),
                                        fieldWithPath("imageUrl").type(STRING).description("이미지 URL"),
                                        fieldWithPath("tagList[]").type(ARRAY).description("태그 목록"),
                                        fieldWithPath("createdAt").type(STRING).description("작성일"),
                                        fieldWithPath("isBookmarked").type(BOOLEAN).description("북마크 여부")
                                )
                ));
    }
}