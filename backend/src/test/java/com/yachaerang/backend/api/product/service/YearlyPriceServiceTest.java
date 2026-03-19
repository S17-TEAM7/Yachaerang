package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.YearlyPriceResponseDto;
import com.yachaerang.backend.api.product.repository.YearlyPriceMapper;
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
class YearlyPriceServiceTest {

    @Mock YearlyPriceMapper yearlyPriceMapper;
    @InjectMocks YearlyPriceService yearlyPriceService;

    private YearlyPriceResponseDto.PriceDto priceDto1;
    private YearlyPriceResponseDto.PriceDto priceDto2;
    private YearlyPriceResponseDto.PriceDto priceDto3;
    private YearlyPriceResponseDto.PriceDto priceDto4;

    @BeforeEach
    void setUp() {
        priceDto1 = YearlyPriceResponseDto.PriceDto.builder()
                .priceYear(2020)
                .avgPrice(100000L)
                .minPrice(90000L)
                .maxPrice(110000L)
                .startPrice(95000L)
                .endPrice(105000L)
                .build();
        priceDto2 = YearlyPriceResponseDto.PriceDto.builder()
                .priceYear(2021)
                .avgPrice(100000L)
                .minPrice(90000L)
                .maxPrice(110000L)
                .startPrice(95000L)
                .endPrice(105000L)
                .build();
        priceDto3 = YearlyPriceResponseDto.PriceDto.builder()
                .priceYear(2022)
                .avgPrice(100000L)
                .minPrice(90000L)
                .maxPrice(110000L)
                .startPrice(95000L)
                .endPrice(105000L)
                .build();
        priceDto4 = YearlyPriceResponseDto.PriceDto.builder()
                .priceYear(2023)
                .avgPrice(110000L)
                .minPrice(100000L)
                .maxPrice(120000L)
                .startPrice(105000L)
                .endPrice(115000L)
                .build();
    }

    @Test
    @DisplayName("연도별 가격 데이터 목록 조회 성공")
    void 연도별_가격_데이터_목록_조회_성공() {
        // given
        String productCode = "PROD001";
        int startYear = 2020;
        int endYear = 2024;

        List<YearlyPriceResponseDto.PriceDto> expectedPrices = List.of(
                priceDto1, priceDto2, priceDto3, priceDto4
        );
        given(yearlyPriceMapper.getPriceDuration(productCode, startYear, endYear))
                .willReturn(expectedPrices);

        // when
        List<YearlyPriceResponseDto.PriceDto> result = yearlyPriceService.getPrices(productCode, startYear, endYear);

        // then
        assertThat(result).isEqualTo(expectedPrices);
        assertThat(result).hasSize(expectedPrices.size());
        verify(yearlyPriceMapper, times(1)).getPriceDuration(productCode, startYear, endYear);
    }

    @Test
    @DisplayName("데이터가 없을 때 빈 리스트")
    void 데이터가_없을때_빈리스트() {
        // given
        String productCode = "PROD001";
        int startYear = 2020;
        int endYear = 2024;

        given(yearlyPriceMapper.getPriceDuration(productCode, startYear, endYear))
                .willReturn(Collections.emptyList());

        // when
        List<YearlyPriceResponseDto.PriceDto> result = yearlyPriceService.getPrices(productCode, startYear, endYear);

        // then
        assertThat(result).isEqualTo(Collections.emptyList());
        assertThat(result).hasSize(0);
        verify(yearlyPriceMapper, times(1)).getPriceDuration(productCode, startYear, endYear);
    }

    @Test
    @DisplayName("단일연도 조회 성공")
    void 단일연도_조회_성공() {
        // given
        String productCode = "PROD001";
        int year = 2020;
        List<YearlyPriceResponseDto.PriceDto> expectedResult = List.of(priceDto1);
        given(yearlyPriceMapper.getPriceDuration(productCode, year, year))
                .willReturn(expectedResult);
        // when
        List<YearlyPriceResponseDto.PriceDto> result = yearlyPriceService.getPrices(productCode, year, year);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(priceDto1);
    }

    @Test
    @DisplayName("특정 연도의 가격 데이터만 조회")
    void 특정연도의_가격데이터만_조회() {
        // given
        String productCode = "PROD001";
        int targetYear = 2020;

        YearlyPriceResponseDto.PriceDto expectedPrice = priceDto1;

        given(yearlyPriceMapper.getPriceSpecification(productCode, targetYear))
                .willReturn(expectedPrice);

        // when
        YearlyPriceResponseDto.PriceDto result = yearlyPriceService.getPrice(
                productCode, targetYear
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedPrice);
        assertThat(result.getPriceYear()).isEqualTo(2020);

        verify(yearlyPriceMapper, times(1))
                .getPriceSpecification(productCode, targetYear);
    }

    // ─── 캐시 레이어 테스트 ────────────────────────────────────────────────────
    // getPrices  캐시 조건: endYear < now().year
    // getPrice   캐시 조건: targetYear < now().year
    @Nested
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = CacheLayerTest.TestConfig.class)
    class CacheLayerTest {

        @Configuration
        @EnableCaching
        static class TestConfig {

            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("yearly:price");
            }

            @Bean
            public YearlyPriceMapper yearlyPriceMapper() {
                return Mockito.mock(YearlyPriceMapper.class);
            }

            @Bean
            public YearlyPriceService yearlyPriceService(YearlyPriceMapper yearlyPriceMapper) {
                return new YearlyPriceService(yearlyPriceMapper);
            }
        }

        @Autowired
        YearlyPriceMapper yearlyPriceMapper;

        @Autowired
        YearlyPriceService yearlyPriceService;

        @Autowired
        CacheManager cacheManager;

        private final int currentYear = LocalDate.now().getYear();
        private final int pastYear = currentYear - 2;

        @BeforeEach
        void resetMocksAndCaches() {
            Mockito.reset(yearlyPriceMapper);
            cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        }

        @Test
        @DisplayName("getPrices: 과거 endYear 기간 조회 시 두 번째 호출은 캐시에서 반환 (매퍼 1회 호출)")
        void getPrices_과거endYear_캐시히트() {
            String productCode = "PROD001";
            // endYear(pastYear) < currentYear → 조건 충족 → 캐시 적용
            given(yearlyPriceMapper.getPriceDuration(productCode, pastYear - 1, pastYear))
                    .willReturn(Collections.emptyList());

            yearlyPriceService.getPrices(productCode, pastYear - 1, pastYear);
            yearlyPriceService.getPrices(productCode, pastYear - 1, pastYear);

            verify(yearlyPriceMapper, times(1)).getPriceDuration(productCode, pastYear - 1, pastYear);
        }

        @Test
        @DisplayName("getPrices: 올해 endYear 기간 조회 시 캐시 조건 미충족으로 매퍼 매번 호출")
        void getPrices_올해endYear_캐시조건_미충족() {
            String productCode = "PROD001";
            // endYear == currentYear → 조건 미충족 → 캐시 미적용
            given(yearlyPriceMapper.getPriceDuration(productCode, pastYear, currentYear))
                    .willReturn(Collections.emptyList());

            yearlyPriceService.getPrices(productCode, pastYear, currentYear);
            yearlyPriceService.getPrices(productCode, pastYear, currentYear);

            verify(yearlyPriceMapper, times(2)).getPriceDuration(productCode, pastYear, currentYear);
        }

        @Test
        @DisplayName("getPrice: 과거 targetYear 조회 시 두 번째 호출은 캐시에서 반환 (매퍼 1회 호출)")
        void getPrice_과거targetYear_캐시히트() {
            String productCode = "PROD001";
            // targetYear(pastYear) < currentYear → 조건 충족 → 캐시 적용
            YearlyPriceResponseDto.PriceDto dto = YearlyPriceResponseDto.PriceDto.builder()
                    .priceYear(pastYear).avgPrice(100000L).build();
            given(yearlyPriceMapper.getPriceSpecification(productCode, pastYear)).willReturn(dto);

            yearlyPriceService.getPrice(productCode, pastYear);
            yearlyPriceService.getPrice(productCode, pastYear);

            verify(yearlyPriceMapper, times(1)).getPriceSpecification(productCode, pastYear);
        }

        @Test
        @DisplayName("getPrice: 올해 targetYear 조회 시 캐시 조건 미충족으로 매퍼 매번 호출")
        void getPrice_올해targetYear_캐시조건_미충족() {
            String productCode = "PROD001";
            // targetYear == currentYear → 조건 미충족 → 캐시 미적용
            given(yearlyPriceMapper.getPriceSpecification(productCode, currentYear)).willReturn(null);

            yearlyPriceService.getPrice(productCode, currentYear);
            yearlyPriceService.getPrice(productCode, currentYear);

            verify(yearlyPriceMapper, times(2)).getPriceSpecification(productCode, currentYear);
        }

        @Test
        @DisplayName("getPrices와 getPrice는 같은 캐시('yearly:price')를 공유하지 않음 (키가 다름)")
        void getPrices와_getPrice_캐시키_독립성() {
            String productCode = "PROD001";
            // getPrices 키: "PROD001:2022:2023", getPrice 키: "PROD001:2022" → 서로 다른 키
            given(yearlyPriceMapper.getPriceDuration(productCode, pastYear, pastYear))
                    .willReturn(Collections.emptyList());
            given(yearlyPriceMapper.getPriceSpecification(productCode, pastYear)).willReturn(null);

            yearlyPriceService.getPrices(productCode, pastYear, pastYear);
            yearlyPriceService.getPrices(productCode, pastYear, pastYear); // 캐시 히트
            yearlyPriceService.getPrice(productCode, pastYear);
            yearlyPriceService.getPrice(productCode, pastYear);            // 캐시 히트

            verify(yearlyPriceMapper, times(1)).getPriceDuration(productCode, pastYear, pastYear);
            verify(yearlyPriceMapper, times(1)).getPriceSpecification(productCode, pastYear);
        }
    }
}
