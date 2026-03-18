package com.yachaerang.backend.infrastructure.batch.client;

import com.yachaerang.backend.infrastructure.batch.dto.response.BatchDailyResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobExecutionResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchWeeklyRangeResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchInternalClientTest {

    @Mock
    private BatchHttpTemplate httpTemplate;

    @InjectMocks
    private BatchInternalClient batchInternalClient;

    @Test
    @DisplayName("일별 Job 실행 시 GET /daily 요청을 전달한다")
    void runDailyJob() {
        // given
        LocalDate targetDate = LocalDate.of(2025, 3, 19);
        BatchDailyResponseDto expected = new BatchDailyResponseDto("COMPLETED", "2025-03-19");
        given(httpTemplate.get(eq("/daily"), eq(BatchDailyResponseDto.class), any()))
                .willReturn(expected);

        // when
        BatchDailyResponseDto result = batchInternalClient.runDailyJob(targetDate);

        // then
        assertThat(result).isSameAs(expected);
        verify(httpTemplate).get(eq("/daily"), eq(BatchDailyResponseDto.class), any());
    }

    @Test
    @DisplayName("날짜 범위 수집 시 POST /date-range 요청을 전달한다")
    void collectDateRange() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        BatchJobExecutionResponseDto expected = new BatchJobExecutionResponseDto(
                true, 1L, "COMPLETED", "2025-01-01", "2025-01-31", null, null, null);
        given(httpTemplate.post(eq("/date-range"), eq(BatchJobExecutionResponseDto.class), any()))
                .willReturn(expected);

        // when
        BatchJobExecutionResponseDto result = batchInternalClient.collectDateRange(startDate, endDate);

        // then
        assertThat(result).isSameAs(expected);
        verify(httpTemplate).post(eq("/date-range"), eq(BatchJobExecutionResponseDto.class), any());
    }

    @Test
    @DisplayName("주별 가격 Job 실행 시 POST /weekly-price 요청을 전달한다")
    void runWeeklyPriceJob() {
        // given
        Integer year = 2025;
        Integer week = 10;
        BatchJobExecutionResponseDto expected = new BatchJobExecutionResponseDto(
                true, 2L, "COMPLETED", null, null, "2025", "10", null);
        given(httpTemplate.post(eq("/weekly-price"), eq(BatchJobExecutionResponseDto.class), any()))
                .willReturn(expected);

        // when
        BatchJobExecutionResponseDto result = batchInternalClient.runWeeklyPriceJob(year, week);

        // then
        assertThat(result).isSameAs(expected);
        verify(httpTemplate).post(eq("/weekly-price"), eq(BatchJobExecutionResponseDto.class), any());
    }

    @Test
    @DisplayName("주별 범위 수집 시 POST /weekly-price/range 요청을 전달한다")
    void collectWeeklyRange() {
        // given
        Integer startYear = 2025;
        Integer startWeek = 1;
        Integer endYear = 2025;
        Integer endWeek = 10;
        BatchWeeklyRangeResponseDto expected = new BatchWeeklyRangeResponseDto(
                true, List.of(1L, 2L, 3L), List.of("COMPLETED", "COMPLETED", "COMPLETED"));
        given(httpTemplate.post(eq("/weekly-price/range"), eq(BatchWeeklyRangeResponseDto.class), any()))
                .willReturn(expected);

        // when
        BatchWeeklyRangeResponseDto result = batchInternalClient.collectWeeklyRange(startYear, startWeek, endYear, endWeek);

        // then
        assertThat(result).isSameAs(expected);
        verify(httpTemplate).post(eq("/weekly-price/range"), eq(BatchWeeklyRangeResponseDto.class), any());
    }

    @Test
    @DisplayName("월별 가격 Job 실행 시 POST /monthly-price 요청을 전달한다")
    void runMonthlyPriceJob() {
        // given
        Integer year = 2025;
        Integer month = 3;
        BatchJobExecutionResponseDto expected = new BatchJobExecutionResponseDto(
                true, 3L, "COMPLETED", null, null, "2025", null, "3");
        given(httpTemplate.post(eq("/monthly-price"), eq(BatchJobExecutionResponseDto.class), any()))
                .willReturn(expected);

        // when
        BatchJobExecutionResponseDto result = batchInternalClient.runMonthlyPriceJob(year, month);

        // then
        assertThat(result).isSameAs(expected);
        verify(httpTemplate).post(eq("/monthly-price"), eq(BatchJobExecutionResponseDto.class), any());
    }

    @Test
    @DisplayName("연별 가격 Job 실행 시 POST /yearly-price 요청을 전달한다")
    void runYearlyPriceJob() {
        // given
        Integer year = 2025;
        BatchJobExecutionResponseDto expected = new BatchJobExecutionResponseDto(
                true, 4L, "COMPLETED", null, null, "2025", null, null);
        given(httpTemplate.post(eq("/yearly-price"), eq(BatchJobExecutionResponseDto.class), any()))
                .willReturn(expected);

        // when
        BatchJobExecutionResponseDto result = batchInternalClient.runYearlyPriceJob(year);

        // then
        assertThat(result).isSameAs(expected);
        verify(httpTemplate).post(eq("/yearly-price"), eq(BatchJobExecutionResponseDto.class), any());
    }
}
