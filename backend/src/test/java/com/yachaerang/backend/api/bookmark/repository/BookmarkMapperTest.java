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
}