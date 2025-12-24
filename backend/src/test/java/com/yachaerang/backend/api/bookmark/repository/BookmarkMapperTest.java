package com.yachaerang.backend.api.bookmark.repository;

import com.yachaerang.backend.api.bookmark.vo.ArticleBookmark;
import com.yachaerang.backend.api.bookmark.entity.Bookmark;
import com.yachaerang.backend.global.config.MyBatisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@ActiveProfiles("test")
@Import({MyBatisConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql("classpath:H2_schema.sql")
class BookmarkMapperTest {

    @Autowired
    private BookmarkMapper bookmarkMapper;

    private Long testMemberId = 1L;
    private Long testArticleId = 1L;

    @Test
    @DisplayName("북마크 저장 성공")
    void 북마크_저장_성공() {
        // given
        Bookmark bookmark = new Bookmark();
        bookmark.setArticleId(testArticleId);
        bookmark.setMemberId(testMemberId);

        // when
        int result = bookmarkMapper.save(bookmark);

        // then
        assertThat(result).isEqualTo(1);
        assertThat(bookmark.getBookmarkId()).isNotNull();
    }

    @Test
    @DisplayName("북마크 삭제 성공")
    void 북마크_삭제_성공() {
        // given
        Bookmark bookmark = new Bookmark();
        bookmark.setArticleId(testArticleId);
        bookmark.setMemberId(testMemberId);
        bookmarkMapper.save(bookmark);

        // when
        int result = bookmarkMapper.delete(testMemberId, testArticleId);

        // then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("memberId와 articleId로 북마크 조회 성공")
    void memberId와_articleId로_북마크_조회_성공() {
        // given
        Bookmark bookmark = Bookmark.builder()
                .articleId(testArticleId)
                .memberId(testMemberId)
                .build();
        bookmarkMapper.save(bookmark);

        // when
        Bookmark found = bookmarkMapper.findByMemberIdAndArticleId(testMemberId, testArticleId);

        // then
        assertThat(found.getArticleId()).isEqualTo(testArticleId);
        assertThat(found.getMemberId()).isEqualTo(testMemberId);
    }


    @Test
    @Sql(scripts = {"/sql/bookmark-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"/sql/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("memberId로 전체 북마크 조회")
    void memberId로_전체_북마크_조회() {
        // given
        // when
        List<ArticleBookmark> bookmarks = bookmarkMapper.findAllByMemberId(testMemberId);

        // then
        assertThat(bookmarks).isNotEmpty();

        ArticleBookmark firstBookmark = bookmarks.get(0);
        assertThat(firstBookmark.getBookmarkId()).isNotNull();
        assertThat(firstBookmark.getMemberId()).isEqualTo(testMemberId);
        assertThat(firstBookmark.getArticleId()).isNotNull();

        // Article 정보 검증
        assertThat(firstBookmark.getArticle()).isNotNull();
        assertThat(firstBookmark.getArticle().getTitle()).isNotNull();
        // Tag 정보 검증
        assertThat(firstBookmark.getArticle().getTagList()).isNotNull();
    }

    @Test
    @Sql(scripts = {"/sql/bookmark-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"/sql/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("memberId로 북마크된 article_id 목록 조회 성공")
    void memberId로_북마크된_articleId_목록_조회_성공() {
        // given
        // when
        Set<Long> articleIds = bookmarkMapper.findArticleIdByMemberId(testMemberId);

        // then
        assertThat(articleIds).isNotEmpty();
        assertThat(articleIds).allMatch(id -> id != null && id > 0);
    }

    @Test
    @DisplayName("memberId로 북마크된 article_id 목록 조회 - 북마크 없음")
    void memberId로_북마크된_articleId_목록_조회_북마크_없음() {
        // given
        Long memberIdWithNoBookmarks = 999L;

        // when
        Set<Long> articleIds = bookmarkMapper.findArticleIdByMemberId(memberIdWithNoBookmarks);

        // then
        assertThat(articleIds).isEmpty();
    }

    @Test
    @DisplayName("북마크 존재하는 경우 존재 여부 확인")
    void 북마크_존재_여부_확인_존재함() {
        // given
        Bookmark bookmark = Bookmark.builder()
                .articleId(testArticleId)
                .memberId(testMemberId)
                .build();
        bookmarkMapper.save(bookmark);

        // when
        Boolean exists = bookmarkMapper.findExistingByMemberIdAndArticleId(testMemberId, testArticleId);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("북마크 존재하지 않는 경우 존재 여부 확인")
    void 북마크_존재_여부_확인_존재하지_않음() {
        // given
        Long nonExistentArticleId = 999L;

        // when
        Boolean exists = bookmarkMapper.findExistingByMemberIdAndArticleId(testMemberId, nonExistentArticleId);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @Sql(scripts = {"/sql/bookmark-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"/sql/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("여러 개의 북마크에서 article_id 목록 조회")
    void 여러_개의_북마크에서_articleId_목록_조회() {
        // given
        Bookmark bookmark2 = Bookmark.builder()
                .articleId(3L)
                .memberId(testMemberId)
                .build();
        Bookmark bookmark3 = Bookmark.builder()
                .articleId(4L)
                .memberId(testMemberId)
                .build();
        bookmarkMapper.save(bookmark2);
        bookmarkMapper.save(bookmark3);

        // when
        Set<Long> articleIds = bookmarkMapper.findArticleIdByMemberId(testMemberId);

        // then
        assertThat(articleIds).hasSizeGreaterThanOrEqualTo(2);
        assertThat(articleIds).contains(2L, 3L);
        assertThat(articleIds).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("북마크 삭제 후 존재 여부 확인 - 존재하지 않음")
    void 북마크_삭제_후_존재_여부_확인() {
        // given
        Bookmark bookmark = Bookmark.builder()
                .articleId(testArticleId)
                .memberId(testMemberId)
                .build();
        bookmarkMapper.save(bookmark);

        // when
        bookmarkMapper.delete(testMemberId, testArticleId);
        Boolean exists = bookmarkMapper.findExistingByMemberIdAndArticleId(testMemberId, testArticleId);

        // then
        assertThat(exists).isFalse();
    }
}