package com.yachaerang.backend.api.bookmark.service;

import com.yachaerang.backend.api.article.entity.Article;
import com.yachaerang.backend.api.article.entity.Tag;
import com.yachaerang.backend.api.bookmark.dto.response.BookmarkResponseDto;
import com.yachaerang.backend.api.bookmark.vo.ArticleBookmark;
import com.yachaerang.backend.api.bookmark.entity.Bookmark;
import com.yachaerang.backend.api.bookmark.repository.BookmarkMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private AuthenticatedMemberProvider authenticatedMemberProvider;

    @Mock
    private BookmarkMapper bookmarkMapper;

    @InjectMocks
    private BookmarkService bookmarkService;

    private Long testMemberId;
    private Long testArticleId;

    @BeforeEach
    void setUp() {
        testMemberId = 1L;
        testArticleId = 100L;
    }

    @Test
    @DisplayName("북마크 저장 성공")
    void 북마크_저장_성공() {
        // given
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findByMemberIdAndArticleId(testMemberId, testArticleId)).thenReturn(null);
        when(bookmarkMapper.save(any(Bookmark.class))).thenReturn(1);

        // when
        bookmarkService.registerBookmark(testArticleId);

        // then
        verify(authenticatedMemberProvider).getCurrentMemberId();
        verify(bookmarkMapper).findByMemberIdAndArticleId(testMemberId, testArticleId);
        verify(bookmarkMapper).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("이미 북마크된 게시글일 경우 실패")
    void 이미_북마크된_게시글일_경우_실패() {
        // given
        Bookmark existingBookmark = Bookmark.builder()
                .articleId(testArticleId)
                .memberId(testMemberId)
                .build();

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findByMemberIdAndArticleId(testMemberId, testArticleId))
                .thenReturn(existingBookmark);

        // when & then
        assertThatThrownBy(() -> bookmarkService.registerBookmark(testArticleId))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_BOOKMARKED);

        verify(bookmarkMapper, never()).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("북마크 DB 저장 실패")
    void 북마크_DB_저장_실패() {
        // given
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findByMemberIdAndArticleId(testMemberId, testArticleId)).thenReturn(null);
        when(bookmarkMapper.save(any(Bookmark.class))).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> bookmarkService.registerBookmark(testArticleId))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKMARK_DB_FAILED);
    }

    @Test
    @DisplayName("북마크 해제 성공")
    void 북마크_해제_성공() {
        // given
        Bookmark existingBookmark = Bookmark.builder()
                .articleId(testArticleId)
                .memberId(testMemberId)
                .build();

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findByMemberIdAndArticleId(testMemberId, testArticleId))
                .thenReturn(existingBookmark);
        when(bookmarkMapper.delete(testMemberId, testArticleId)).thenReturn(1);

        // when
        bookmarkService.eraseBookmark(testArticleId);

        // then
        verify(authenticatedMemberProvider).getCurrentMemberId();
        verify(bookmarkMapper).findByMemberIdAndArticleId(testMemberId, testArticleId);
        verify(bookmarkMapper).delete(testMemberId, testArticleId);
    }

    @Test
    @DisplayName("북마크가 존재하지 않을경우 해제 실패")
    void 북마크가_존재하지_않을경우_해제_실패() {
        // given
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findByMemberIdAndArticleId(testMemberId, testArticleId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> bookmarkService.eraseBookmark(testArticleId))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKMARK_NOT_FOUND);

        verify(bookmarkMapper, never()).delete(anyLong(), anyLong());
    }

    @Test
    @DisplayName("북마크 DB 삭제 실패")
    void 북마크_DB_삭제_실패() {
        // given
        Bookmark existingBookmark = Bookmark.builder()
                .articleId(testArticleId)
                .memberId(testMemberId)
                .build();

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findByMemberIdAndArticleId(testMemberId, testArticleId))
                .thenReturn(existingBookmark);
        when(bookmarkMapper.delete(testMemberId, testArticleId)).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> bookmarkService.eraseBookmark(testArticleId))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKMARK_DB_FAILED);
    }

    @Test
    @DisplayName("북마크 전체 태그와 함께 조회 성공")
    void 북마크_전체_태그와_함께_조회_성공() {
        // given
        Tag tag1 = new Tag();
        tag1.setName("Spring");
        Tag tag2 = new Tag();
        tag2.setName("Java");

        Article article = new Article();
        article.setTitle("테스트 게시글");
        article.setTagList(Arrays.asList(tag1, tag2));

        ArticleBookmark articleBookmark = new ArticleBookmark();
        articleBookmark.setBookmarkId(1L);
        articleBookmark.setArticleId(testArticleId);
        articleBookmark.setMemberId(testMemberId);
        articleBookmark.setArticle(article);

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findAllByMemberId(testMemberId))
                .thenReturn(Collections.singletonList(articleBookmark));

        // when
        List<BookmarkResponseDto.GetAllDto> result = bookmarkService.getAll();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookmarkId()).isEqualTo(1L);
        assertThat(result.get(0).getArticleId()).isEqualTo(testArticleId);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 게시글");
        assertThat(result.get(0).getTagList()).containsExactly("Spring", "Java");

        verify(authenticatedMemberProvider).getCurrentMemberId();
        verify(bookmarkMapper).findAllByMemberId(testMemberId);
    }

    @Test
    @DisplayName("태그 없을 때 북마크 전체 조회 성공")
    void 태그_없을때_북마크_전체_조회_성공() {
        // given
        Article article = new Article();
        article.setTitle("태그 없는 게시글");
        article.setTagList(null);

        ArticleBookmark articleBookmark = new ArticleBookmark();
        articleBookmark.setBookmarkId(2L);
        articleBookmark.setArticleId(testArticleId);
        articleBookmark.setMemberId(testMemberId);
        articleBookmark.setArticle(article);

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findAllByMemberId(testMemberId))
                .thenReturn(Collections.singletonList(articleBookmark));

        // when
        List<BookmarkResponseDto.GetAllDto> result = bookmarkService.getAll();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTagList()).isEmpty();
    }

    @Test
    @DisplayName("북마크가 없는경우 전체 조회 성공")
    void 북마크가_없는경우_전체조회_성공() {
        // given
        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findAllByMemberId(testMemberId))
                .thenReturn(Collections.emptyList());

        // when
        List<BookmarkResponseDto.GetAllDto> result = bookmarkService.getAll();

        // then
        assertThat(result).isEmpty();
        verify(authenticatedMemberProvider).getCurrentMemberId();
        verify(bookmarkMapper).findAllByMemberId(testMemberId);
    }

    @Test
    @DisplayName("북마크 전체 조회 성공")
    void 조회_성공_여러_북마크() {
        // given
        Article article1 = new Article();
        article1.setTitle("첫 번째 게시글");
        article1.setTagList(null);

        Article article2 = new Article();
        article2.setTitle("두 번째 게시글");
        Tag tag = new Tag();
        tag.setName("MyBatis");
        article2.setTagList(Collections.singletonList(tag));

        ArticleBookmark bookmark1 = new ArticleBookmark();
        bookmark1.setBookmarkId(1L);
        bookmark1.setArticleId(100L);
        bookmark1.setArticle(article1);

        ArticleBookmark bookmark2 = new ArticleBookmark();
        bookmark2.setBookmarkId(2L);
        bookmark2.setArticleId(200L);
        bookmark2.setArticle(article2);

        when(authenticatedMemberProvider.getCurrentMemberId()).thenReturn(testMemberId);
        when(bookmarkMapper.findAllByMemberId(testMemberId))
                .thenReturn(Arrays.asList(bookmark1, bookmark2));

        // when
        List<BookmarkResponseDto.GetAllDto> result = bookmarkService.getAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(result.get(1).getTitle()).isEqualTo("두 번째 게시글");
        assertThat(result.get(1).getTagList()).containsExactly("MyBatis");
    }
}