package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.DailyPriceResponseDto;
import com.yachaerang.backend.api.product.repository.DailyPriceMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Transactional
@ExtendWith(MockitoExtension.class)
class DailyPriceServiceTest {

    @Mock
    DailyPriceMapper dailyPriceMapper;

    @InjectMocks
    DailyPriceService dailyPriceService;

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final ZonedDateTime FIXED_NOW = ZonedDateTime.of(
            2024, 1, 15, 10, 0, 0, 0, SEOUL_ZONE
    );
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
        try (MockedStatic<ZonedDateTime> mockedZonedDateTime = mockStatic(ZonedDateTime.class, CALLS_REAL_METHODS)) {
            // given
            // any(ZoneId.class)를 사용하여 ZoneId 매칭 문제 해결
            mockedZonedDateTime.when(() -> ZonedDateTime.now(any(ZoneId.class)))
                    .thenReturn(FIXED_NOW);

            given(dailyPriceMapper.getPricesDescending(YESTERDAY))
                    .willReturn(createDescendingMockData());

            // when
            List<DailyPriceResponseDto.RankDto> result = dailyPriceService.getHighPriceRank();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getProductName()).isEqualTo("수박");
            assertThat(result.get(0).getPrice()).isEqualTo(20000);

            verify(dailyPriceMapper).getPricesDescending(YESTERDAY);
        }
    }

    @Test
    @DisplayName("데이터가 없으면 빈 리스트")
    void 데이터가_없으면_빈리스트() {
        try (MockedStatic<ZonedDateTime> mockedZonedDateTime = mockStatic(ZonedDateTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedZonedDateTime.when(() -> ZonedDateTime.now(SEOUL_ZONE)).thenReturn(FIXED_NOW);

            given(dailyPriceMapper.getPricesDescending(YESTERDAY))
                    .willReturn(Collections.emptyList());

            // when
            List<DailyPriceResponseDto.RankDto> result = dailyPriceService.getHighPriceRank();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("어제 날짜를 기준으로 가격 오름차순")
    void 어제날짜를_기준으로_가격_오름차순() {
        try (MockedStatic<ZonedDateTime> mockedZonedDateTime = mockStatic(ZonedDateTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedZonedDateTime.when(() -> ZonedDateTime.now(SEOUL_ZONE)).thenReturn(FIXED_NOW);

            given(dailyPriceMapper.getPricesAscending(YESTERDAY))
                    .willReturn(createAscendingMockData());

            // when
            List<DailyPriceResponseDto.RankDto> result = dailyPriceService.getLowPriceRank();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getProductName()).isEqualTo("감귤");
            assertThat(result.get(0).getPrice()).isEqualTo(2000L);

            verify(dailyPriceMapper).getPricesAscending(YESTERDAY);
        }
    }
}