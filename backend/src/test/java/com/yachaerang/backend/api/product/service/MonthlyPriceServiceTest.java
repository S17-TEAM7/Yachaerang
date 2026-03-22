package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.MonthlyPriceResponseDto;
import com.yachaerang.backend.api.product.repository.MonthlyPriceMapper;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonthlyPriceServiceTest {

    @Mock MonthlyPriceMapper monthlyPriceMapper;
    @InjectMocks MonthlyPriceService monthlyPriceService;

    @Test
    @DisplayName("월별 가격 데이터 반환 성공")
    void 월별가격데이터를_반환_성공() {
        // given
        String productCode = "PRODUCT01";
        int startYear = 2024;
        int startMonth = 1;
        int endYear = 2024;
        int endMonth = 6;

        List<MonthlyPriceResponseDto.PriceDto> expectedResult = List.of(
                MonthlyPriceResponseDto.PriceDto.builder()
                        .priceYear(2024)
                        .priceMonth(1)
                        .avgPrice(10000L)
                        .minPrice(9000L)
                        .maxPrice(11000L)
                        .build(),
                MonthlyPriceResponseDto.PriceDto.builder()
                        .priceYear(2024)
                        .priceMonth(2)
                        .avgPrice(11000L)
                        .minPrice(10000L)
                        .maxPrice(12000L)
                        .build(),
                MonthlyPriceResponseDto.PriceDto.builder()
                        .priceYear(2024)
                        .priceMonth(3)
                        .avgPrice(90000L)
                        .minPrice(8000L)
                        .maxPrice(10000L)
                        .build()
        );
        given(monthlyPriceMapper.getPriceDuration(productCode, startYear, startMonth, endYear, endMonth))
                .willReturn(expectedResult);

        // when
        List<MonthlyPriceResponseDto.PriceDto> result = monthlyPriceService.getMonthlyPrices(
                productCode, startYear, startMonth, endYear, endMonth
        );

        // then
        assertThat(result).hasSize(expectedResult.size());
        assertThat(result).isEqualTo(expectedResult);
        verify(monthlyPriceMapper, times(1))
                .getPriceDuration(productCode, startYear, startMonth, endYear, endMonth);
    }

    @Test
    @DisplayName("데이터가 없으면 빈 리스트")
    void 데이터가_없으면_빈리스트(){
        // given
        String productCode = "PRODUCT01";
        int startYear = 2024;
        int startMonth = 1;
        int endYear = 2024;
        int endMonth = 6;
        given(monthlyPriceMapper.getPriceDuration(productCode, startYear, startMonth, endYear, endMonth))
                .willReturn(Collections.emptyList());

        // when
        List<MonthlyPriceResponseDto.PriceDto> result = monthlyPriceService.getMonthlyPrices(
                productCode, startYear, startMonth, endYear, endMonth
        );

        // then
        assertThat(result).hasSize(0);
        verify(monthlyPriceMapper, times(1))
                .getPriceDuration(productCode, startYear, startMonth, endYear, endMonth);
    }

    @Test
    @DisplayName("단일 월 조회 성공")
    void 단일_월_조회_성공() {
        // given
        String productCode = "PROD001";
        int year = 2024;
        int month = 5;

        List<MonthlyPriceResponseDto.PriceDto> expectedPrices = List.of(
                MonthlyPriceResponseDto.PriceDto.builder()
                        .priceYear(2024)
                        .priceMonth(5)
                        .avgPrice(10000L)
                        .minPrice(9000L)
                        .maxPrice(11000L)
                        .build()
        );

        given(monthlyPriceMapper.getPriceDuration(productCode, year, month, year, month))
                .willReturn(expectedPrices);

        // when
        List<MonthlyPriceResponseDto.PriceDto> result = monthlyPriceService.getMonthlyPrices(
                productCode, year, month, year, month
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPriceYear()).isEqualTo(2024);
        assertThat(result.get(0).getPriceMonth()).isEqualTo(5);
    }

    // ─── 캐시 레이어 테스트 ────────────────────────────────────────────────────
    // 캐시 조건: endYear < now().year  OR  (endYear == now().year AND endMonth < now().month)
    @Nested
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = CacheLayerTest.TestConfig.class)
    class CacheLayerTest {

        @Configuration
        @EnableCaching
        static class TestConfig {

            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("monthly:price");
            }

            @Bean
            public MonthlyPriceMapper monthlyPriceMapper() {
                return Mockito.mock(MonthlyPriceMapper.class);
            }

            @Bean
            public MonthlyPriceService monthlyPriceService(MonthlyPriceMapper monthlyPriceMapper) {
                return new MonthlyPriceService(monthlyPriceMapper);
            }
        }

        @Autowired
        MonthlyPriceMapper monthlyPriceMapper;

        @Autowired
        MonthlyPriceService monthlyPriceService;

        @Autowired
        CacheManager cacheManager;

        @BeforeEach
        void resetMocksAndCaches() {
            Mockito.reset(monthlyPriceMapper);
            cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        }

        @Test
        @DisplayName("과거 연도 기간 조회: 두 번째 호출은 캐시에서 반환 (매퍼 1회 호출)")
        void 과거연도기간_두번째호출_캐시히트() {
            String productCode = "PROD001";
            // endYear(2024) < currentYear(2026) → 조건 충족 → 캐시 적용
            int startYear = 2024, startMonth = 1, endYear = 2024, endMonth = 6;

            given(monthlyPriceMapper.getPriceDuration(productCode, startYear, startMonth, endYear, endMonth))
                    .willReturn(Collections.emptyList());

            monthlyPriceService.getMonthlyPrices(productCode, startYear, startMonth, endYear, endMonth);
            monthlyPriceService.getMonthlyPrices(productCode, startYear, startMonth, endYear, endMonth);

            verify(monthlyPriceMapper, times(1))
                    .getPriceDuration(productCode, startYear, startMonth, endYear, endMonth);
        }

        @Test
        @DisplayName("현재 연월 기간 조회: 캐시 조건 미충족으로 매퍼 매번 호출")
        void 현재연월_기간_캐시조건_미충족() {
            String productCode = "PROD001";
            int currentYear = LocalDate.now().getYear();
            int currentMonth = LocalDate.now().getMonthValue();
            // endYear == currentYear AND endMonth == currentMonth → 조건 미충족 → 캐시 미적용
            int startYear = currentYear, startMonth = 1;

            given(monthlyPriceMapper.getPriceDuration(productCode, startYear, startMonth, currentYear, currentMonth))
                    .willReturn(Collections.emptyList());

            monthlyPriceService.getMonthlyPrices(productCode, startYear, startMonth, currentYear, currentMonth);
            monthlyPriceService.getMonthlyPrices(productCode, startYear, startMonth, currentYear, currentMonth);

            verify(monthlyPriceMapper, times(2))
                    .getPriceDuration(productCode, startYear, startMonth, currentYear, currentMonth);
        }

        @Test
        @DisplayName("같은 연도 이전 월 기간 조회: 캐시 조건 충족")
        void 같은연도_이전월_기간_캐시조건_충족() {
            String productCode = "PROD001";
            int currentYear = LocalDate.now().getYear();
            int previousMonth = LocalDate.now().minusMonths(2).getMonthValue();
            int previousMonthYear = LocalDate.now().minusMonths(2).getYear();

            // endYear == currentYear AND endMonth < currentMonth → 조건 충족 → 캐시 적용
            given(monthlyPriceMapper.getPriceDuration(productCode, previousMonthYear, 1, previousMonthYear, previousMonth))
                    .willReturn(Collections.emptyList());

            monthlyPriceService.getMonthlyPrices(productCode, previousMonthYear, 1, previousMonthYear, previousMonth);
            monthlyPriceService.getMonthlyPrices(productCode, previousMonthYear, 1, previousMonthYear, previousMonth);

            verify(monthlyPriceMapper, times(1))
                    .getPriceDuration(productCode, previousMonthYear, 1, previousMonthYear, previousMonth);
        }

        @Test
        @DisplayName("다른 productCode는 별도 캐시 키 → 각각 매퍼 호출")
        void 다른productCode_별도캐시키() {
            String productCode1 = "PROD001";
            String productCode2 = "PROD002";

            given(monthlyPriceMapper.getPriceDuration(productCode1, 2024, 1, 2024, 6))
                    .willReturn(Collections.emptyList());
            given(monthlyPriceMapper.getPriceDuration(productCode2, 2024, 1, 2024, 6))
                    .willReturn(Collections.emptyList());

            monthlyPriceService.getMonthlyPrices(productCode1, 2024, 1, 2024, 6);
            monthlyPriceService.getMonthlyPrices(productCode2, 2024, 1, 2024, 6);

            verify(monthlyPriceMapper, times(1)).getPriceDuration(productCode1, 2024, 1, 2024, 6);
            verify(monthlyPriceMapper, times(1)).getPriceDuration(productCode2, 2024, 1, 2024, 6);
        }
    }
}
