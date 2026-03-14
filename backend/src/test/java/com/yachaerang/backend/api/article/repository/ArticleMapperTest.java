package com.yachaerang.backend.api.article.repository;

import com.yachaerang.backend.api.article.entity.Article;
import com.yachaerang.backend.global.config.MyBatisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql("classpath:H2_schema.sql")
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@SqlConfig(encoding = "UTF-8")
@Import(MyBatisConfig.class)
class ArticleMapperTest {

    @Autowired ArticleMapper articleMapper;

    @Test
    @DisplayName("findById 성공")
    @Sql(scripts = "/sql/article-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findById_성공() {
        // given
        Long articleId = 1L;

        // when
        Article article = articleMapper.findById(articleId);

        // then
        assertThat(article).isNotNull();
        assertThat(article.getId()).isEqualTo(articleId);
        assertThat(article.getTitle()).isNotNull();
        assertThat(article.getStatus()).isEqualTo(Article.Status.pending);
        assertThat(article.getImageCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("findById 존재하지 않는 ID로 조회 시 null")
    void findById_존재하지않는_ID로_조회시_null() {
        // given
        Long nonExistingId = 99999L;

        // when
        Article article = articleMapper.findById(nonExistingId);

        // then
        assertThat(article).isNull();
    }
    @Test
    @DisplayName("findAllWithPagination 첫 번째 페이지 조회")
    @Sql(scripts = "/sql/article-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findAllWithPagination_첫번째_페이지_조회() {
        // given
        int limit = 5;
        int offset = 0;

        // when
        List<Article> articles = articleMapper.findAllWithPagination(limit, offset);

        // then
        assertThat(articles).hasSize(5);
    }

    @Test
    @DisplayName("findAllWithPagination 두 번째 페이지 조회")
    @Sql(scripts = "/sql/article-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findAllWithPagination_두번째_페이지_조회() {
        // given
        int limit = 5;
        int offset = 5; // page 2

        // when
        List<Article> articles = articleMapper.findAllWithPagination(limit, offset);

        // then
        assertThat(articles).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("findAllWithPagination limit 변경으로 size 조정")
    @Sql(scripts = "/sql/article-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findAllWithPagination_limit_변경으로_size_조정() {
        // given
        int limit = 3;
        int offset = 0;

        // when
        List<Article> articles = articleMapper.findAllWithPagination(limit, offset);

        // then
        assertThat(articles).hasSize(3);
    }

    @Test
    @DisplayName("findAllWithPagination offset이 전체 개수보다 크면 빈 리스트")
    @Sql(scripts = "/sql/article-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findAllWithPagination_offset이_전체개수보다_크면_빈리스트() {
        // given
        int limit = 5;
        int offset = 10000;

        // when
        List<Article> articles = articleMapper.findAllWithPagination(limit, offset);

        // then
        assertThat(articles).isEmpty();
    }

    @Test
    @DisplayName("findAllWithPagination 내림차순 정렬")
    @Sql(scripts = "/sql/article-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void findAllWithPagination_내림차순_정렬() {
        // given
        int limit = 10;
        int offset = 0;

        // when
        List<Article> articles = articleMapper.findAllWithPagination(limit, offset);

        // then
        assertThat(articles).hasSizeGreaterThan(1);
        for (int i = 0; i < articles.size() - 1; i++) {
            assertThat(articles.get(i).getCreatedAt())
                    .isAfterOrEqualTo(articles.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("countAll 성공")
    @Sql(scripts = "/sql/article-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void countAll_성공() {
        // when
        Long count = articleMapper.countAll();

        // then
        assertThat(count).isNotNull();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("페이지네이션 전체 개수 일관")
    @Sql(scripts = "/sql/article-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void 페이지네이션_전체_개수_일관() {
        // given
        int limit = 5;
        Long totalCount = articleMapper.countAll();
        int expectedTotalPages = (int) Math.ceil((double) totalCount / limit);

        // when & then
        int actualTotalItems = 0;
        for (int page = 1; page <= expectedTotalPages; page++) {
            int offset = (page - 1) * limit;
            List<Article> articles = articleMapper.findAllWithPagination(limit, offset);
            actualTotalItems += articles.size();
        }

        assertThat((long) actualTotalItems).isEqualTo(totalCount);
    }


    @Test
    @DisplayName("키워드로 기사 검색 성공")
    @Sql(scripts = "/sql/article-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void 기사_키워드_검색_성공() {
        // given
        int limit = 10;
        int offset = 0;
        String keyword = "기사제목10";

        // when
        List<Article> articles =
                articleMapper.findByKeyword(limit, offset, keyword);

        // then
        assertThat(articles).isNotNull();
        assertThat(articles).isNotEmpty();
        assertThat(articles.size()).isLessThanOrEqualTo(limit);
        // 검색 결과 -> 키워드 포함 확인
        articles.forEach(article ->
                assertThat(
                        article.getTitle().contains(keyword)
                                || article.getContent().contains(keyword)
                ).isTrue()
        );
    }

    @Test
    @DisplayName("키워드 검색 결과가 없을 때 빈 리스트 반환")
    void 기사_키워드_검색_결과없음() {
        // given
        int limit = 10;
        int offset = 0;
        String keyword = "존재하지않는키워드";

        // when
        List<Article> articles =
                articleMapper.findByKeyword(limit, offset, keyword);

        // then
        assertThat(articles).isNotNull();
        assertThat(articles).isEmpty();
    }

    @Test
    @DisplayName("키워드 검색 기사 개수 조회")
    void 기사_키워드_개수_조회() {
        // given
        String keyword = "환경";

        // when
        Long count = articleMapper.countByKeyword(keyword);

        // then
        assertThat(count).isNotNull();
        assertThat(count).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("페이징 동작 확인")
    void 기사_검색_페이징_동작_확인() {
        // given
        String keyword = "환경";
        int limit = 5;

        // when
        List<Article> firstPage =
                articleMapper.findByKeyword(limit, 0, keyword);
        List<Article> secondPage =
                articleMapper.findByKeyword(limit, limit, keyword);

        // then
        if (!firstPage.isEmpty() && !secondPage.isEmpty()) {
            assertThat(firstPage.get(0).getId())
                    .isNotEqualTo(secondPage.get(0).getId());
        }
    }
}