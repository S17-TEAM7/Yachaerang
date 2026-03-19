package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.WeeklyPriceResponseDto;
import com.yachaerang.backend.api.product.repository.WeeklyPriceMapper;
import com.yachaerang.backend.global.exception.CustomException;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyPriceServiceTest {

    @Mock
    WeeklyPriceMapper weeklyPriceMapper;

    @InjectMocks
    WeeklyPriceService weeklyPriceService;

    private String productCode;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        productCode = "PROD001";
        today = LocalDate.of(2025, 11, 11);
    }

    @Test
    @DisplayName("유효한 날짜로 기간 조회 성공")
    void 유효한날짜로_기간_조회_성공() {
        // given
        LocalDate startDate = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endDate = startDate.plusDays(13);
        List<WeeklyPriceResponseDto.PriceRecordDto> expectedResult = List.of(
                WeeklyPriceResponseDto.PriceRecordDto.builder()
                        .startDate(startDate)
                        .endDate(startDate.plusDays(6))
                        .avgPrice(1000L)
                        .minPrice(700L)
                        .maxPrice(1300L)
                        .build(),
                WeeklyPriceResponseDto.PriceRecordDto.builder()
                        .startDate(startDate.plusDays(7))
                        .endDate(endDate)
                        .avgPrice(1100L)
                        .minPrice(700L)
                        .maxPrice(1400L)
                        .build()
        );
        when(weeklyPriceMapper.getPriceDuration(productCode, startDate, endDate)).thenReturn(expectedResult);

        // when
        List<WeeklyPriceResponseDto.PriceRecordDto> result =
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedResult);
        verify(weeklyPriceMapper).getPriceDuration(productCode, startDate, endDate);
    }

    @Test
    @DisplayName("endDate가 startDate보다 이전일 때 실패")
    void endDate가_startDate보다_이전_실패() {
        // given
        LocalDate startDate = today.minusDays(today.getDayOfWeek().getValue() -1);
        LocalDate endDate = startDate.minusDays(1); // 지난주 일요일

        // when & then
        assertThatThrownBy(() ->
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate))
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException ex = (GeneralException) exception;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WRONG_REQUEST_DATE);
                });

        verify(weeklyPriceMapper, never()).getPriceDuration(any(), any(), any());
    }

    @Test
    @DisplayName("시작일이 월요일이 아니면 예외")
    void 시작일이_월요일이_아니면_예외() {
        // given
        LocalDate startDate = today.minusDays(today.getDayOfWeek().getValue() - 2); // 화요일
        LocalDate endDate = startDate.plusDays(5);

        // when & then
        assertThatThrownBy(() ->
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_ISO_WEEK_DATE);
                });
        verify(weeklyPriceMapper, never()).getPriceDuration(any(), any(), any());
    }

    @Test
    @DisplayName("종료일이 일요일이 아니면 예외")
    void 종료일이_일요일이_아니면_예외() {
        // given
        LocalDate startDate = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endDate = startDate.plusDays(21);

        // when & then
        assertThatThrownBy(() ->
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_ISO_WEEK_DATE);
                });
        verify(weeklyPriceMapper, never()).getPriceDuration(any(), any(), any());
    }

    @Test
    @DisplayName("주간의 시작일과 종료일 성공")
    void 주간의_시작일과_종료일_성공() {
        // given
        LocalDate startDate = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endDate = startDate.plusDays(6);
        List<WeeklyPriceResponseDto.PriceRecordDto> expectedResult = List.of(
                WeeklyPriceResponseDto.PriceRecordDto.builder()
                        .startDate(startDate)
                        .endDate(startDate.plusDays(6))
                        .avgPrice(1000L)
                        .minPrice(700L)
                        .maxPrice(1300L)
                        .build()
        );
        when(weeklyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                .thenReturn(expectedResult);

        // when
        List<WeeklyPriceResponseDto.PriceRecordDto> result = weeklyPriceService.getPriceDuration(productCode, startDate, endDate);

        // then
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(expectedResult);
        verify(weeklyPriceMapper).getPriceDuration(productCode, startDate, endDate);
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 리스트를 반환")
    void 조회결과_없으면_빈리스트_반환_성공() {
        // given
        LocalDate startDate = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endDate = startDate.plusDays(6);

        when(weeklyPriceMapper.getPriceDuration(eq(productCode), any(), any()))
                .thenReturn(Collections.emptyList());

        // when
        List<WeeklyPriceResponseDto.PriceRecordDto> result =
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ISO 기준 2025년 1주차: 2024-12-30(월) ~ 2025-01-05(일) 조회 성공")
    void shouldSucceedForIsoWeek1Of2025CrossingYearBoundary() {
        // given
        String productCode = "PRODUCT001";
        // ISO 기준 2025년 1주차는 2024년 12월 30일(월)에 시작
        LocalDate startDate = LocalDate.of(2024, 12, 30); // 월요일
        LocalDate endDate = LocalDate.of(2025, 1, 5);     // 일요일

        // 날짜 검증
        assertThat(startDate.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(endDate.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);

        // ISO 주차 검증
        WeekFields weekFields = WeekFields.ISO;
        assertThat(startDate.get(weekFields.weekOfWeekBasedYear())).isEqualTo(1);
        assertThat(startDate.get(weekFields.weekBasedYear())).isEqualTo(2025);
        assertThat(endDate.get(weekFields.weekOfWeekBasedYear())).isEqualTo(1);
        assertThat(endDate.get(weekFields.weekBasedYear())).isEqualTo(2025);

        List<WeeklyPriceResponseDto.PriceRecordDto> expectedResult = List.of(
                WeeklyPriceResponseDto.PriceRecordDto.builder()
                        .startDate(startDate)
                        .endDate(startDate.plusDays(6))
                        .avgPrice(1000L)
                        .minPrice(700L)
                        .maxPrice(1300L)
                        .build()
        );

        when(weeklyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                .thenReturn(expectedResult);

        // when
        List<WeeklyPriceResponseDto.PriceRecordDto> result =
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(weeklyPriceMapper).getPriceDuration(productCode, startDate, endDate);
    }

    @Test
    @DisplayName("ISO 기준 2024년 52주차에서 2025년 1주차까지 여러 주 조회 성공")
    void shouldSucceedForMultipleWeeksAcrossYearBoundary() {
        // given
        String productCode = "PRODUCT001";
        // 2024년 52주차 시작: 2024-12-23 (월)
        // 2025년 1주차 종료: 2025-01-05 (일)
        LocalDate startDate = LocalDate.of(2024, 12, 23); // 월요일, 2024년 52주차
        LocalDate endDate = LocalDate.of(2025, 1, 5);     // 일요일, 2025년 1주차

        // 날짜 검증
        assertThat(startDate.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(endDate.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);

        // ISO 주차 검증
        WeekFields weekFields = WeekFields.ISO;
        assertThat(startDate.get(weekFields.weekOfWeekBasedYear())).isEqualTo(52);
        assertThat(startDate.get(weekFields.weekBasedYear())).isEqualTo(2024);

        List<WeeklyPriceResponseDto.PriceRecordDto> expectedResult = List.of(
                WeeklyPriceResponseDto.PriceRecordDto.builder()
                        .startDate(LocalDate.of(2024, 12, 23))
                        .endDate(LocalDate.of(2025, 1, 5))
                        .avgPrice(1000L)
                        .minPrice(700L)
                        .maxPrice(1300L)
                        .build()
        );

        when(weeklyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                .thenReturn(expectedResult);

        // when
        List<WeeklyPriceResponseDto.PriceRecordDto> result =
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate);

        // then
        assertThat(result).hasSize(1);
        verify(weeklyPriceMapper).getPriceDuration(productCode, startDate, endDate);
    }

    @Test
    @DisplayName("2020년처럼 53주차가 있는 해의 연도 경계 조회 성공")
    void shouldSucceedForYearWith53Weeks() {
        // given
        String productCode = "PRODUCT001";
        // 2020년은 53주차가 있는 해
        // 2020년 53주차: 2020-12-28(월) ~ 2021-01-03(일)
        LocalDate startDate = LocalDate.of(2020, 12, 28); // 월요일, 2020년 53주차
        LocalDate endDate = LocalDate.of(2021, 1, 3);     // 일요일, 2020년 53주차

        // 날짜 검증
        assertThat(startDate.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(endDate.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);

        // ISO 주차 검증 - 둘 다 2020년 53주차
        WeekFields weekFields = WeekFields.ISO;
        assertThat(startDate.get(weekFields.weekOfWeekBasedYear())).isEqualTo(53);
        assertThat(startDate.get(weekFields.weekBasedYear())).isEqualTo(2020);
        assertThat(endDate.get(weekFields.weekOfWeekBasedYear())).isEqualTo(53);
        assertThat(endDate.get(weekFields.weekBasedYear())).isEqualTo(2020);

        List<WeeklyPriceResponseDto.PriceRecordDto> expectedResult = List.of(
                WeeklyPriceResponseDto.PriceRecordDto.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .avgPrice(1000L)
                        .minPrice(700L)
                        .maxPrice(1300L)
                        .build()
        );

        when(weeklyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                .thenReturn(expectedResult);

        // when
        List<WeeklyPriceResponseDto.PriceRecordDto> result =
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate);

        // then
        assertThat(result).isEqualTo(expectedResult);

        // 추가 검증: 이 주간에 포함된 모든 날짜가 2020년 마지막 주차임을 확인
        for (int i = 0; i <= 6; i++) {
            LocalDate date = startDate.plusDays(i);
            assertThat(date.get(weekFields.weekBasedYear())).isEqualTo(2020);
            assertThat(date.get(weekFields.weekOfWeekBasedYear())).isEqualTo(53);
        }
    }

    @Test
    @DisplayName("1월 1일이 목요일인 해의 1주차 조회 - 전년도 12월 29일부터 시작")
    void shouldSucceedWhenJan1IsThursday() {
        // given
        String productCode = "PRODUCT001";
        // 2015년 1월 1일은 목요일
        // ISO 기준 2015년 1주차: 2014-12-29(월) ~ 2015-01-04(일)
        LocalDate startDate = LocalDate.of(2014, 12, 29); // 월요일
        LocalDate endDate = LocalDate.of(2015, 1, 4);     // 일요일

        // 날짜 검증
        assertThat(startDate.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(endDate.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(LocalDate.of(2015, 1, 1).getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);

        // ISO 주차 검증 - 둘 다 2015년 1주차
        WeekFields weekFields = WeekFields.ISO;
        assertThat(startDate.get(weekFields.weekOfWeekBasedYear())).isEqualTo(1);
        assertThat(startDate.get(weekFields.weekBasedYear())).isEqualTo(2015);
        assertThat(endDate.get(weekFields.weekOfWeekBasedYear())).isEqualTo(1);
        assertThat(endDate.get(weekFields.weekBasedYear())).isEqualTo(2015);

        List<WeeklyPriceResponseDto.PriceRecordDto> expectedResult = List.of(
                WeeklyPriceResponseDto.PriceRecordDto.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .avgPrice(1000L)
                        .minPrice(700L)
                        .maxPrice(1300L)
                        .build()
        );

        when(weeklyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                .thenReturn(expectedResult);

        // when
        List<WeeklyPriceResponseDto.PriceRecordDto> result =
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate);

        // then
        assertThat(result).isEqualTo(expectedResult);

        // 추가 검증: 이 주간에 포함된 모든 날짜가 2015년 1주차임을 확인
        for (int i = 0; i <= 6; i++) {
            LocalDate date = startDate.plusDays(i);
            assertThat(date.get(weekFields.weekBasedYear())).isEqualTo(2015);
            assertThat(date.get(weekFields.weekOfWeekBasedYear())).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("1월 1일이 금/토/일인 경우 1월 1일이 전년도 마지막 주차에 속한다")
    void shouldHandleJan1InPreviousYearLastWeek() {
        // given
        String productCode = "PRODUCT001";
        // 2022년 1월 1일은 토요일이므로 2021년 52주차에 속함
        // 2021년 52주차: 2021-12-27(월) ~ 2022-01-02(일)
        LocalDate startDate = LocalDate.of(2021, 12, 27); // 월요일
        LocalDate endDate = LocalDate.of(2022, 1, 2);     // 일요일

        // 날짜 검증
        assertThat(startDate.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(endDate.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(LocalDate.of(2022, 1, 1).getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);

        // ISO 주차 검증 - 둘 다 2021년 52주차
        WeekFields weekFields = WeekFields.ISO;
        assertThat(startDate.get(weekFields.weekOfWeekBasedYear())).isEqualTo(52);
        assertThat(startDate.get(weekFields.weekBasedYear())).isEqualTo(2021);

        // 2022년 1월 1일(토)도 2021년 52주차에 속함
        LocalDate jan1_2022 = LocalDate.of(2022, 1, 1);
        assertThat(jan1_2022.get(weekFields.weekOfWeekBasedYear())).isEqualTo(52);
        assertThat(jan1_2022.get(weekFields.weekBasedYear())).isEqualTo(2021);

        List<WeeklyPriceResponseDto.PriceRecordDto> expectedResult = List.of(
                WeeklyPriceResponseDto.PriceRecordDto.builder()
                        .startDate(LocalDate.of(2021, 12, 27))
                        .endDate(LocalDate.of(2022, 1, 2))
                        .avgPrice(1000L)
                        .minPrice(700L)
                        .maxPrice(1300L)
                        .build()
        );

        when(weeklyPriceMapper.getPriceDuration(productCode, startDate, endDate))
                .thenReturn(expectedResult);

        // when
        List<WeeklyPriceResponseDto.PriceRecordDto> result =
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("연도 경계에서 시작일이 월요일이 아니면 예외가 발생")
    void 연도경계에서_시작일이_월요일이_아니면_예외가발생() {
        // given
        // 2024년 12월 31일(화)은 월요일이 아님
        LocalDate startDate = LocalDate.of(2024, 12, 31); // 화요일
        LocalDate endDate = LocalDate.of(2025, 1, 5);     // 일요일

        assertThat(startDate.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);

        // when & then
        assertThatThrownBy(() ->
                weeklyPriceService.getPriceDuration(productCode, startDate, endDate))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_ISO_WEEK_DATE);
                });

        verify(weeklyPriceMapper, never()).getPriceDuration(any(), any(), any());
    }

    // ─── 캐시 레이어 테스트 ────────────────────────────────────────────────────
    // WeeklyPriceService 캐시 조건: endDate.isBefore(now)
    // 서비스 자체 검증: endDate.isAfter(today) → 예외 발생
    // → endDate == today(일요일)일 때만 캐시 미적용 시나리오가 유효하므로, 과거 주간(캐시 적용)만 검증
    @Nested
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = WeeklyPriceServiceTest.CacheLayerTest.TestConfig.class)
    class CacheLayerTest {

        @Configuration
        @EnableCaching
        static class TestConfig {

            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("weekly:price");
            }

            @Bean
            public WeeklyPriceMapper weeklyPriceMapper() {
                return Mockito.mock(WeeklyPriceMapper.class);
            }

            @Bean
            public WeeklyPriceService weeklyPriceService(WeeklyPriceMapper weeklyPriceMapper) {
                return new WeeklyPriceService(weeklyPriceMapper);
            }
        }

        @Autowired
        WeeklyPriceMapper weeklyPriceMapper;

        @Autowired
        WeeklyPriceService weeklyPriceService;

        @Autowired
        CacheManager cacheManager;

        // 2025-11-10(월) ~ 2025-11-16(일): 과거 주간, 날짜 검증 통과
        private static final LocalDate PAST_START = LocalDate.of(2025, 11, 10);
        private static final LocalDate PAST_END = LocalDate.of(2025, 11, 16);

        @BeforeEach
        void resetMocksAndCaches() {
            Mockito.reset(weeklyPriceMapper);
            cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        }

        @Test
        @DisplayName("과거 주간 조회: 두 번째 호출은 캐시에서 반환 (매퍼 1회 호출)")
        void 과거주간_두번째호출_캐시히트() {
            String productCode = "PROD001";
            List<WeeklyPriceResponseDto.PriceRecordDto> data = List.of(
                    WeeklyPriceResponseDto.PriceRecordDto.builder()
                            .startDate(PAST_START).endDate(PAST_END)
                            .avgPrice(10000L).minPrice(8000L).maxPrice(12000L).build()
            );
            given(weeklyPriceMapper.getPriceDuration(productCode, PAST_START, PAST_END)).willReturn(data);

            weeklyPriceService.getPriceDuration(productCode, PAST_START, PAST_END); // 캐시 미스
            weeklyPriceService.getPriceDuration(productCode, PAST_START, PAST_END); // 캐시 히트

            verify(weeklyPriceMapper, times(1)).getPriceDuration(productCode, PAST_START, PAST_END);
        }

        @Test
        @DisplayName("다른 productCode는 별도 캐시 키 → 각각 매퍼 호출")
        void 다른productCode_별도캐시키() {
            String productCode1 = "PROD001";
            String productCode2 = "PROD002";

            given(weeklyPriceMapper.getPriceDuration(productCode1, PAST_START, PAST_END))
                    .willReturn(Collections.emptyList());
            given(weeklyPriceMapper.getPriceDuration(productCode2, PAST_START, PAST_END))
                    .willReturn(Collections.emptyList());

            weeklyPriceService.getPriceDuration(productCode1, PAST_START, PAST_END);
            weeklyPriceService.getPriceDuration(productCode2, PAST_START, PAST_END);

            verify(weeklyPriceMapper, times(1)).getPriceDuration(productCode1, PAST_START, PAST_END);
            verify(weeklyPriceMapper, times(1)).getPriceDuration(productCode2, PAST_START, PAST_END);
        }

        @Test
        @DisplayName("다른 기간은 별도 캐시 키 → 각각 매퍼 호출")
        void 다른기간_별도캐시키() {
            String productCode = "PROD001";
            // 2025-11-03(월) ~ 2025-11-09(일): 또 다른 과거 주간
            LocalDate otherStart = LocalDate.of(2025, 11, 3);
            LocalDate otherEnd = LocalDate.of(2025, 11, 9);

            given(weeklyPriceMapper.getPriceDuration(productCode, PAST_START, PAST_END))
                    .willReturn(Collections.emptyList());
            given(weeklyPriceMapper.getPriceDuration(productCode, otherStart, otherEnd))
                    .willReturn(Collections.emptyList());

            weeklyPriceService.getPriceDuration(productCode, PAST_START, PAST_END);
            weeklyPriceService.getPriceDuration(productCode, otherStart, otherEnd);

            verify(weeklyPriceMapper, times(1)).getPriceDuration(productCode, PAST_START, PAST_END);
            verify(weeklyPriceMapper, times(1)).getPriceDuration(productCode, otherStart, otherEnd);
        }
    }
}
