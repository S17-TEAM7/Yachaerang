package com.yachaerang.backend.api.article.service;

import com.yachaerang.backend.api.article.dto.request.ArticleRequestDto;
import com.yachaerang.backend.api.article.dto.response.ArticleResponseDto;
import com.yachaerang.backend.api.article.entity.Article;
import com.yachaerang.backend.api.article.entity.Tag;
import com.yachaerang.backend.api.article.repository.ArticleMapper;
import com.yachaerang.backend.api.article.repository.TagMapper;
import com.yachaerang.backend.api.bookmark.repository.BookmarkMapper;
import com.yachaerang.backend.global.auth.jwt.AuthenticatedMemberProvider;
import com.yachaerang.backend.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @InjectMocks
    private ArticleService articleService;

    @Mock
    private AuthenticatedMemberProvider authenticatedMemberProvider;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private BookmarkMapper bookmarkMapper;

    private Article article1;
    private Article article2;
    private Tag tag1;
    private Tag tag2;
    private Tag tag3;
    private Tag tag4;

    private static final String TEST_KEYWORD = "title";
    private static final int TEST_SIZE = 5;
    private static final int TEST_PAGE = 1;

    private ArticleRequestDto.PageDto pageRequest;

    @BeforeEach
    void setUp() {
        tag1 = Tag.builder().id(1L).name("tag1").articleId(1L).build();
        tag2 = Tag.builder().id(2L).name("tag2").articleId(1L).build();
        tag3 = Tag.builder().id(3L).name("tag3").articleId(2L).build();
        tag4 = Tag.builder().id(4L).name("tag4").articleId(2L).build();

        article1 = new Article(1L, "title1", "content1", "default.png", "http://example.com",
                List.of(tag1, tag2));
        article1.setCreatedAt(LocalDateTime.of(2020, 11, 11, 11, 11));

        article2 = new Article(2L, "title2", "content2", "default.png", "http://example.com",
                List.of(tag3, tag4));
        article2.setCreatedAt(LocalDateTime.of(2020, 11, 12, 11, 11));

        pageRequest = ArticleRequestDto.PageDto.builder()
                .size(TEST_SIZE)
                .page(TEST_PAGE)
                .build();
    }

    @Nested
    @DisplayName("비인증 사용자")
    class unauthorizedMember {

        @Test
        @DisplayName("기사 목록 조회 성공")
        void 기사_목록_조회_성공() {
            // given
            ArticleRequestDto.PageDto pageRequest = ArticleRequestDto.PageDto.builder()
                    .page(1)
                    .size(5)
                    .build();

            List<Article> articleList = List.of(article1, article2);
            List<Tag> tagList = List.of(tag1, tag2, tag3, tag4);

            given(articleMapper.findAllWithPagination(5, 0)).willReturn(articleList);
            given(tagMapper.findByArticleIdList(List.of(1L, 2L))).willReturn(tagList);
            given(articleMapper.countAll()).willReturn(10L);

            // 비인증
            given(authenticatedMemberProvider.isAuthenticated()).willReturn(false);

            // when
            ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> result
                    = articleService.getAllArticles(pageRequest);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(10L);
            assertThat(result.getCurrentPage()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(5);

            // 첫 번째 기사의 태그 확인
            assertThat(result.getContent().get(0).getTagList())
                    .containsExactly("tag1", "tag2");

            // 두 번째 기사의 태그 확인
            assertThat(result.getContent().get(1).getTagList())
                    .containsExactly("tag3", "tag4");

            // 비인증이면 북마크 무조건 false
            assertThat(result.getContent().get(0).getIsBookmarked()).isFalse();
            assertThat(result.getContent().get(1).getIsBookmarked()).isFalse();

            verify(articleMapper).findAllWithPagination(5, 0);
            verify(articleMapper).countAll();
            verify(tagMapper).findByArticleIdList(List.of(1L, 2L));

            // 비인증이므로 북마크 조회 자체를 하지 않음
            verify(bookmarkMapper, never()).findArticleIdByMemberId(anyLong());
        }

        @Test
        @DisplayName("특정 기사 정상 조회")
        void 기사_상세_조회_성공_비인증() {
            // given
            Long articleId = 1L;
            List<String> tagNameList = List.of("tag1", "tag2");

            given(tagMapper.findNameByArticleId(articleId)).willReturn(tagNameList);
            given(articleMapper.findById(articleId)).willReturn(article1);

            given(authenticatedMemberProvider.isAuthenticated()).willReturn(false);

            // when
            ArticleResponseDto.DetailDto result = articleService.getArticle(articleId);

            // then
            assertThat(result.getArticleId()).isEqualTo(articleId);
            assertThat(result.getTitle()).isEqualTo("title1");
            assertThat(result.getContent()).isEqualTo("content1");
            assertThat(result.getTagList()).containsExactly("tag1", "tag2");
            assertThat(result.getIsBookmarked()).isFalse();

            verify(articleMapper).findById(articleId);
            verify(tagMapper).findNameByArticleId(articleId);

            verify(bookmarkMapper, never()).findExistingByMemberIdAndArticleId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("기사 검색 성공")
        void 기사_검색_성공_비인증() {
            // given
            given(articleMapper.findByKeyword(pageRequest.getLimit(), pageRequest.getOffset(), TEST_KEYWORD))
                    .willReturn(List.of(article1, article2));

            given(articleMapper.countByKeyword(TEST_KEYWORD))
                    .willReturn(2L);

            given(tagMapper.findByArticleIdList(List.of(1L, 2L)))
                    .willReturn(List.of(tag1, tag2, tag3, tag4));

            given(authenticatedMemberProvider.isAuthenticated()).willReturn(false);

            // when
            ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> result =
                    articleService.searchArticles(pageRequest, TEST_KEYWORD);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2L);

            List<ArticleResponseDto.ListDto> content = result.getContent();
            assertThat(content).hasSize(2);

            assertThat(content.get(0).getTagList()).containsExactlyInAnyOrder("tag1", "tag2");
            assertThat(content.get(1).getTagList()).containsExactlyInAnyOrder("tag3", "tag4");

            assertThat(content.get(0).getIsBookmarked()).isFalse();
            assertThat(content.get(1).getIsBookmarked()).isFalse();

            verify(bookmarkMapper, never()).findArticleIdByMemberId(anyLong());
        }
    }

    @Nested
    @DisplayName("인증 사용자")
    class authorizedMember {

        @Test
        @DisplayName("기사 목록 조회 성공")
        void 기사_목록_조회_성공_인증() {
            // given
            ArticleRequestDto.PageDto pageRequest = ArticleRequestDto.PageDto.builder()
                    .page(1)
                    .size(5)
                    .build();

            List<Article> articleList = List.of(article1, article2);
            List<Tag> tagList = List.of(tag1, tag2, tag3, tag4);

            given(articleMapper.findAllWithPagination(5, 0)).willReturn(articleList);
            given(articleMapper.countAll()).willReturn(2L);
            given(tagMapper.findByArticleIdList(List.of(1L, 2L))).willReturn(tagList);

            // 인증 + memberId
            given(authenticatedMemberProvider.isAuthenticated()).willReturn(true);
            given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(100L);

            // article1만 북마크되어있다고 가정
            given(bookmarkMapper.findArticleIdByMemberId(100L)).willReturn(Set.of(1L));

            // when
            ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> result =
                    articleService.getAllArticles(pageRequest);

            // then
            assertThat(result.getContent()).hasSize(2);

            assertThat(result.getContent().get(0).getArticleId()).isEqualTo(1L);
            assertThat(result.getContent().get(0).getIsBookmarked()).isTrue();

            assertThat(result.getContent().get(1).getArticleId()).isEqualTo(2L);
            assertThat(result.getContent().get(1).getIsBookmarked()).isFalse();

            verify(bookmarkMapper).findArticleIdByMemberId(100L);
        }


        @Test
        @DisplayName("특정 기사 정상 조회")
        void 기사_상세_조회_성공_인증_북마크반영() {
            // given
            Long articleId = 1L;
            List<String> tagNameList = List.of("tag1", "tag2");

            given(tagMapper.findNameByArticleId(articleId)).willReturn(tagNameList);
            given(articleMapper.findById(articleId)).willReturn(article1);

            given(authenticatedMemberProvider.isAuthenticated()).willReturn(true);
            given(authenticatedMemberProvider.getCurrentMemberId()).willReturn(100L);

            given(bookmarkMapper.findExistingByMemberIdAndArticleId(100L, articleId)).willReturn(true);

            // when
            ArticleResponseDto.DetailDto result = articleService.getArticle(articleId);

            // then
            assertThat(result.getIsBookmarked()).isTrue();
            verify(bookmarkMapper).findExistingByMemberIdAndArticleId(100L, articleId);
        }
    }

    @Test
    @DisplayName("기사가 없을 때 빈 리스트 반환")
    void 기사_목록_조회_빈_리스트() {
        // given
        ArticleRequestDto.PageDto pageRequest = ArticleRequestDto.PageDto.builder()
                .page(1)
                .size(5)
                .build();

        given(articleMapper.findAllWithPagination(5, 0)).willReturn(Collections.emptyList());

        // when
        ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> result
                = articleService.getAllArticles(pageRequest);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);

        verify(articleMapper).findAllWithPagination(5, 0);
        verify(tagMapper, never()).findByArticleIdList(anyList());
        verify(articleMapper, times(1)).countAll();
    }

    @Test
    @DisplayName("태그가 없는 기사도 정상 조회")
    void 기사_목록_조회_태그_없음() {
        // given
        ArticleRequestDto.PageDto pageRequest = ArticleRequestDto.PageDto.builder()
                .page(1)
                .size(5)
                .build();

        given(articleMapper.findAllWithPagination(5, 0)).willReturn(List.of(article1));
        given(tagMapper.findByArticleIdList(List.of(1L))).willReturn(Collections.emptyList());
        given(articleMapper.countAll()).willReturn(1L);

        // when
        ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> result
                = articleService.getAllArticles(pageRequest);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTagList()).isEmpty();
    }

    @Test
    @DisplayName("두 번째 페이지 조회 시 offset 계산 확인")
    void 기사_목록_조회_두번째_페이지() {
        // given
        ArticleRequestDto.PageDto pageRequest = ArticleRequestDto.PageDto.builder()
                .page(2)
                .size(5)
                .build();

        given(articleMapper.findAllWithPagination(5, 5)).willReturn(List.of(article1));
        given(tagMapper.findByArticleIdList(List.of(1L))).willReturn(List.of(tag1, tag2));
        given(articleMapper.countAll()).willReturn(10L);

        // when
        ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> result
                = articleService.getAllArticles(pageRequest);

        // then
        assertThat(result.getCurrentPage()).isEqualTo(2);
        verify(articleMapper).findAllWithPagination(5, 5);  // offset = (2-1) * 5 = 5
    }

    @Test
    @DisplayName("존재하지 않는 기사 조회 시 예외 발생")
    void 기사_상세_조회_실패_존재하지_않음() {
        // given
        Long articleId = 999L;

        given(articleMapper.findById(articleId)).willReturn(null);
        given(tagMapper.findNameByArticleId(articleId)).willReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> articleService.getArticle(articleId))
                .isInstanceOf(GeneralException.class);

        verify(articleMapper).findById(articleId);
    }

    @Test
    @DisplayName("태그 없는 기사 정상 조회")
    void 기사_상세_조회_태그_없음() {
        // given
        Long articleId = 1L;

        given(articleMapper.findById(articleId)).willReturn(article1);
        given(tagMapper.findNameByArticleId(articleId)).willReturn(Collections.emptyList());

        // when
        ArticleResponseDto.DetailDto result = articleService.getArticle(articleId);

        // then
        assertThat(result.getArticleId()).isEqualTo(articleId);
        assertThat(result.getTagList()).isEmpty();
    }

    @Test
    @DisplayName("기사 검색 성공")
    void 기사_검색_성공() {
        // given
        given(articleMapper.findByKeyword(pageRequest.getLimit(), pageRequest.getOffset(), TEST_KEYWORD))
                .willReturn(List.of(article1, article2));

        given(articleMapper.countByKeyword(TEST_KEYWORD))
                .willReturn(2L);
        // 태그 다시 조회
        given(tagMapper.findByArticleIdList(List.of(1L, 2L)))
                .willReturn(List.of(tag1, tag2, tag3, tag4));

        // when
        ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> result =
                articleService.searchArticles(pageRequest, TEST_KEYWORD);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2L);

        List<ArticleResponseDto.ListDto> content = result.getContent();
        assertThat(content).hasSize(2);

        ArticleResponseDto.ListDto dto1 = content.get(0);
        assertThat(dto1.getArticleId()).isEqualTo(1L);
        assertThat(dto1.getTitle()).isEqualTo("title1");
        assertThat(dto1.getImageUrl()).isEqualTo("default.png");
        assertThat(dto1.getCreatedAt()).isEqualTo(LocalDateTime.of(2020, 11, 11, 11, 11).toLocalDate());
        assertThat(dto1.getTagList()).containsExactlyInAnyOrder("tag1", "tag2");

        ArticleResponseDto.ListDto dto2 = content.get(1);
        assertThat(dto2.getArticleId()).isEqualTo(2L);
        assertThat(dto2.getTitle()).isEqualTo("title2");
        assertThat(dto2.getImageUrl()).isEqualTo("default.png");
        assertThat(dto2.getCreatedAt()).isEqualTo(LocalDateTime.of(2020, 11, 12, 11, 11).toLocalDate());
        assertThat(dto2.getTagList()).containsExactlyInAnyOrder("tag3", "tag4");

        verify(articleMapper).findByKeyword(pageRequest.getLimit(), pageRequest.getOffset(), TEST_KEYWORD);
        verify(articleMapper).countByKeyword(TEST_KEYWORD);
        verify(tagMapper).findByArticleIdList(List.of(1L, 2L));
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 리스트 반환")
    void 검색_결과가_없으면_빈리스트() {
        // given
        given(articleMapper.findByKeyword(pageRequest.getLimit(), pageRequest.getOffset(), TEST_KEYWORD))
                .willReturn(Collections.emptyList());

        given(articleMapper.countByKeyword(TEST_KEYWORD))
                .willReturn(0L);

        // when
        ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> result =
                articleService.searchArticles(pageRequest, TEST_KEYWORD);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);

        verify(articleMapper).findByKeyword(pageRequest.getLimit(), pageRequest.getOffset(), TEST_KEYWORD);
        verify(articleMapper).countByKeyword(TEST_KEYWORD);
        verify(tagMapper, never()).findByArticleIdList(anyList());
    }

    @Test
    @DisplayName("태그 조회 결과가 비면 tagList 빈 리스트")
    void 태그_조회_결과가_비면_tagList_빈리스트() {
        // given
        given(articleMapper.findByKeyword(pageRequest.getLimit(), pageRequest.getOffset(), TEST_KEYWORD))
                .willReturn(List.of(article1));

        given(articleMapper.countByKeyword(TEST_KEYWORD))
                .willReturn(1L);

        given(tagMapper.findByArticleIdList(List.of(1L)))
                .willReturn(Collections.emptyList());

        // when
        ArticleResponseDto.PageDto<ArticleResponseDto.ListDto> result =
                articleService.searchArticles(pageRequest, TEST_KEYWORD);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);

        ArticleResponseDto.ListDto dto = result.getContent().get(0);
        assertThat(dto.getArticleId()).isEqualTo(1L);
        assertThat(dto.getTagList()).isEmpty();

        verify(tagMapper).findByArticleIdList(List.of(1L));
    }
}