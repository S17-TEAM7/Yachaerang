package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.DailyPriceResponseDto;
import com.yachaerang.backend.api.product.repository.DailyPriceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyPriceServiceTest {

    @Mock
    DailyPriceMapper dailyPriceMapper;

    @InjectMocks
    DailyPriceService dailyPriceService;

    // 리팩터링 후 날짜 계산은 컨트롤러에서 수행하며, 서비스는 date 파라미터를 받음
    private static final LocalDate YESTERDAY = LocalDate.of(2024, 1, 14);

    private List<DailyPriceResponseDto.RankDto> createDescendingMockData() {
        return List.of(
                DailyPriceResponseDto.RankDto.builder()
                        .productCode("WATERMELON")
                        .productName("수박")
                        .itemName("과일")
                        .kindName("수박")
                        .unit("개")
                        .price(20000L)
                        .build(),
                DailyPriceResponseDto.RankDto.builder()
                        .productCode("GRAPE")
                        .productName("포도")
                        .itemName("과일")
                        .kindName("포도")
                        .unit("kg")
                        .price(15000L)
                        .build());
    }

    private List<DailyPriceResponseDto.RankDto> createAscendingMockData() {
        return List.of(
                DailyPriceResponseDto.RankDto.builder()
                        .productCode("ORANGE")
                        .productName("감귤")
                        .itemName("과일")
                        .kindName("감귤")
                        .unit("kg")
                        .price(2000L)
                        .build(),
                DailyPriceResponseDto.RankDto.builder()
                        .productCode("PLUM")
                        .productName("자두")
                        .itemName("과일")
                        .kindName("자두")
                        .unit("kg")
                        .price(4000L)
                        .build());
    }

    @Test
    @DisplayName("기간 내 가격 목록 반환 성공")
    void 기간내가격목록_반환_성공() {
        // given
        String productCode = "PROD001";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        List<DailyPriceResponseDto.PriceRecordDto> expectedPrices = List.of(
                DailyPriceResponseDto.PriceRecordDto.builder()
                        .priceDate(LocalDate.of(2024, 1, 10))
                        .price(10000L)
                        .build(),
                DailyPriceResponseDto.PriceRecordDto.builder()
                        .priceDate(LocalDate.of(2024, 1, 15))
                        .price(10500L)
                        .build(),
                DailyPriceResponseDto.PriceRecordDto.builder()
                        .priceDate(LocalDate.of(2024, 1, 20))
                        .price(9800L)
                        .build()
        );

        given(dailyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                .willReturn(expectedPrices);

        // when
        List<DailyPriceResponseDto.PriceRecordDto> result =
                dailyPriceService.getDailyPrice(productCode, startDate, endDate);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPriceDate()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(result.get(0).getPrice()).isEqualTo(10000L);
        assertThat(result.get(1).getPriceDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.get(1).getPrice()).isEqualTo(10500L);
        assertThat(result.get(2).getPriceDate()).isEqualTo(LocalDate.of(2024, 1, 20));
        assertThat(result.get(2).getPrice()).isEqualTo(9800L);

        verify(dailyPriceMapper, times(1)).getPriceDuration(productCode, startDate, endDate);
    }

    @Test
    @DisplayName("빈 목록 반환 성공")
    void 빈목록_반환_성공() {
        // given
        String productCode = "PROD001";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        given(dailyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                .willReturn(Collections.emptyList());

        // when
        List<DailyPriceResponseDto.PriceRecordDto> result =
                dailyPriceService.getDailyPrice(productCode, startDate, endDate);

        // then
        assertThat(result).isEmpty();
        verify(dailyPriceMapper, times(1)).getPriceDuration(productCode, startDate, endDate);
    }

    @Test
    @DisplayName("하루 조회 성공")
    void 하루_조회_성공() {
        // given
        String productCode = "PROD001";
        LocalDate singleDate = LocalDate.of(2024, 1, 15);

        List<DailyPriceResponseDto.PriceRecordDto> expectedPrice = List.of(
                DailyPriceResponseDto.PriceRecordDto.builder()
                        .priceDate(singleDate)
                        .price(10000L)
                        .build()
        );

        given(dailyPriceMapper.getPriceDuration(productCode, singleDate, singleDate))
                .willReturn(expectedPrice);

        // when
        List<DailyPriceResponseDto.PriceRecordDto> result =
                dailyPriceService.getDailyPrice(productCode, singleDate, singleDate);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPriceDate()).isEqualTo(singleDate);

        verify(dailyPriceMapper).getPriceDuration(productCode, singleDate, singleDate);
    }

    @Test
    @DisplayName("존재하지 않는 productCode 빈 목록 반환")
    void 존재하지않는_productCode_빈목록_반환() {
        // given
        String productCode = "NON_EXISTENT";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        given(dailyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                .willReturn(Collections.emptyList());

        // when
        List<DailyPriceResponseDto.PriceRecordDto> result =
                dailyPriceService.getDailyPrice(productCode, startDate, endDate);

        // then
        assertThat(result).isEmpty();
        verify(dailyPriceMapper).getPriceDuration(productCode, startDate, endDate);
    }

    @Test
    @DisplayName("어제 날짜를 기준으로 가격 내림차순")
    void 어제날짜를_기준으로_가격_내림차순() {
        // given - 날짜 계산은 컨트롤러에서 수행 후 파라미터로 전달
        given(dailyPriceMapper.getPricesDescending(YESTERDAY))
                .willReturn(createDescendingMockData());

        // when
        List<DailyPriceResponseDto.RankDto> result = dailyPriceService.getHighPriceRank(YESTERDAY);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductName()).isEqualTo("수박");
        assertThat(result.get(0).getPrice()).isEqualTo(20000);

        verify(dailyPriceMapper).getPricesDescending(YESTERDAY);
    }

    @Test
    @DisplayName("데이터가 없으면 빈 리스트")
    void 데이터가_없으면_빈리스트() {
        // given
        given(dailyPriceMapper.getPricesDescending(YESTERDAY))
                .willReturn(Collections.emptyList());

        // when
        List<DailyPriceResponseDto.RankDto> result = dailyPriceService.getHighPriceRank(YESTERDAY);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("어제 날짜를 기준으로 가격 오름차순")
    void 어제날짜를_기준으로_가격_오름차순() {
        // given
        given(dailyPriceMapper.getPricesAscending(YESTERDAY))
                .willReturn(createAscendingMockData());

        // when
        List<DailyPriceResponseDto.RankDto> result = dailyPriceService.getLowPriceRank(YESTERDAY);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductName()).isEqualTo("감귤");
        assertThat(result.get(0).getPrice()).isEqualTo(2000L);

        verify(dailyPriceMapper).getPricesAscending(YESTERDAY);
    }

    // ─── 캐시 레이어 테스트 ────────────────────────────────────────────────────
    // @Cacheable AOP는 Spring 프록시가 있어야 동작하므로 SpringExtension + ConcurrentMapCache 사용
    @Nested
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = CacheLayerTest.TestConfig.class)
    class CacheLayerTest {

        @Configuration
        @EnableCaching
        static class TestConfig {

            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager(
                        "daily:price", "daily:rank:high", "daily:rank:low");
            }

            @Bean
            public DailyPriceMapper dailyPriceMapper() {
                return Mockito.mock(DailyPriceMapper.class);
            }

            @Bean
            public DailyPriceService dailyPriceService(DailyPriceMapper dailyPriceMapper) {
                return new DailyPriceService(dailyPriceMapper);
            }
        }

        @Autowired
        DailyPriceMapper dailyPriceMapper;

        @Autowired
        DailyPriceService dailyPriceService;

        @Autowired
        CacheManager cacheManager;

        @BeforeEach
        void resetMocksAndCaches() {
            Mockito.reset(dailyPriceMapper);
            cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        }

        @Test
        @DisplayName("과거 기간 조회: 두 번째 호출은 캐시에서 반환 (매퍼 1회 호출)")
        void 과거기간_두번째호출_캐시히트() {
            String productCode = "PROD001";
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31); // 과거 → condition = true → 캐시 적용

            List<DailyPriceResponseDto.PriceRecordDto> data = List.of(
                    DailyPriceResponseDto.PriceRecordDto.builder()
                            .priceDate(startDate).price(10000L).build()
            );
            given(dailyPriceMapper.getPriceDuration(productCode, startDate, endDate)).willReturn(data);

            dailyPriceService.getDailyPrice(productCode, startDate, endDate); // 캐시 미스 → 매퍼 호출
            dailyPriceService.getDailyPrice(productCode, startDate, endDate); // 캐시 히트 → 매퍼 미호출

            verify(dailyPriceMapper, times(1)).getPriceDuration(productCode, startDate, endDate);
        }

        @Test
        @DisplayName("오늘 날짜가 endDate인 기간: 캐시 조건 미충족으로 매퍼 매번 호출")
        void 오늘이_endDate인_기간_캐시조건_미충족() {
            String productCode = "PROD001";
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.now(); // 오늘 → endDate.isBefore(now) = false → 캐시 미적용

            given(dailyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                    .willReturn(Collections.emptyList());

            dailyPriceService.getDailyPrice(productCode, startDate, endDate);
            dailyPriceService.getDailyPrice(productCode, startDate, endDate);

            verify(dailyPriceMapper, times(2)).getPriceDuration(productCode, startDate, endDate);
        }

        @Test
        @DisplayName("랭크 조회: 동일 날짜 두 번 호출 시 캐시 히트 (매퍼 1회 호출)")
        void 랭크조회_동일날짜_캐시히트() {
            LocalDate date = LocalDate.of(2024, 1, 14);
            List<DailyPriceResponseDto.RankDto> data = List.of(
                    DailyPriceResponseDto.RankDto.builder()
                            .productCode("APPLE").productName("사과")
                            .itemName("과일").kindName("사과")
                            .unit("kg").price(5000L).build()
            );
            given(dailyPriceMapper.getPricesDescending(date)).willReturn(data);

            dailyPriceService.getHighPriceRank(date);
            dailyPriceService.getHighPriceRank(date);

            verify(dailyPriceMapper, times(1)).getPricesDescending(date);
        }

        @Test
        @DisplayName("랭크 조회: 다른 날짜는 별도 캐시 키 → 각각 매퍼 호출")
        void 랭크조회_다른날짜_별도캐시키() {
            LocalDate date1 = LocalDate.of(2024, 1, 13);
            LocalDate date2 = LocalDate.of(2024, 1, 14);

            given(dailyPriceMapper.getPricesDescending(date1)).willReturn(Collections.emptyList());
            given(dailyPriceMapper.getPricesDescending(date2)).willReturn(Collections.emptyList());

            dailyPriceService.getHighPriceRank(date1);
            dailyPriceService.getHighPriceRank(date2);

            verify(dailyPriceMapper, times(1)).getPricesDescending(date1);
            verify(dailyPriceMapper, times(1)).getPricesDescending(date2);
        }

        @Test
        @DisplayName("내림차순·오름차순 랭크는 별도 캐시('daily:rank:high', 'daily:rank:low') 사용")
        void 고가랭크_저가랭크_별도캐시() {
            LocalDate date = LocalDate.of(2024, 1, 14);

            given(dailyPriceMapper.getPricesDescending(date)).willReturn(Collections.emptyList());
            given(dailyPriceMapper.getPricesAscending(date)).willReturn(Collections.emptyList());

            dailyPriceService.getHighPriceRank(date);
            dailyPriceService.getHighPriceRank(date); // 캐시 히트
            dailyPriceService.getLowPriceRank(date);
            dailyPriceService.getLowPriceRank(date);  // 캐시 히트

            verify(dailyPriceMapper, times(1)).getPricesDescending(date);
            verify(dailyPriceMapper, times(1)).getPricesAscending(date);
        }
    }
}
