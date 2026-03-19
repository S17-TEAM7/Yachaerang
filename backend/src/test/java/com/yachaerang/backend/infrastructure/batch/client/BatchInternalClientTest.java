package com.yachaerang.backend.infrastructure.batch.client;

import com.yachaerang.backend.infrastructure.batch.dto.response.BatchDailyResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobExecutionResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchWeeklyRangeResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchInternalClientTest {

    @Mock
    private BatchHttpTemplate httpTemplate;

    @InjectMocks
    private BatchInternalClient batchInternalClient;

    @Test
    @DisplayName("일별 Job 실행 시 GET /daily 요청을 전달하고 targetDate 쿼리 파라미터를 설정한다")
    @SuppressWarnings("unchecked")
    void runDailyJob() {
        // given
        LocalDate targetDate = LocalDate.of(2025, 3, 19);
        BatchDailyResponseDto expected = new BatchDailyResponseDto("COMPLETED", "2025-03-19");
        ArgumentCaptor<UnaryOperator<UriBuilder>> uriCaptor = ArgumentCaptor.forClass(UnaryOperator.class);
        given(httpTemplate.get(eq("/daily"), eq(BatchDailyResponseDto.class), uriCaptor.capture()))
                .willReturn(expected);

        // when
        BatchDailyResponseDto result = batchInternalClient.runDailyJob(targetDate);

        // then
        assertThat(result).isSameAs(expected);
        UriBuilder mockUriBuilder = mock(UriBuilder.class, Answers.RETURNS_SELF);
        uriCaptor.getValue().apply(mockUriBuilder);
        verify(mockUriBuilder).queryParam("targetDate", targetDate);
    }

    @Test
    @DisplayName("날짜 범위 수집 시 POST /date-range 요청을 전달하고 startDate, endDate 쿼리 파라미터를 설정한다")
    @SuppressWarnings("unchecked")
    void collectDateRange() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        BatchJobExecutionResponseDto expected = new BatchJobExecutionResponseDto(
                true, 1L, "COMPLETED", "2025-01-01", "2025-01-31", null, null, null);
        ArgumentCaptor<UnaryOperator<UriBuilder>> uriCaptor = ArgumentCaptor.forClass(UnaryOperator.class);
        given(httpTemplate.post(eq("/date-range"), eq(BatchJobExecutionResponseDto.class), uriCaptor.capture()))
                .willReturn(expected);

        // when
        BatchJobExecutionResponseDto result = batchInternalClient.collectDateRange(startDate, endDate);

        // then
        assertThat(result).isSameAs(expected);
        UriBuilder mockUriBuilder = mock(UriBuilder.class, Answers.RETURNS_SELF);
        uriCaptor.getValue().apply(mockUriBuilder);
        verify(mockUriBuilder).queryParam("startDate", startDate);
        verify(mockUriBuilder).queryParam("endDate", endDate);
    }

    @Test
    @DisplayName("주별 가격 Job 실행 시 POST /weekly-price 요청을 전달하고 year, week 쿼리 파라미터를 설정한다")
    @SuppressWarnings("unchecked")
    void runWeeklyPriceJob() {
        // given
        Integer year = 2025;
        Integer week = 10;
        BatchJobExecutionResponseDto expected = new BatchJobExecutionResponseDto(
                true, 2L, "COMPLETED", null, null, "2025", "10", null);
        ArgumentCaptor<UnaryOperator<UriBuilder>> uriCaptor = ArgumentCaptor.forClass(UnaryOperator.class);
        given(httpTemplate.post(eq("/weekly-price"), eq(BatchJobExecutionResponseDto.class), uriCaptor.capture()))
                .willReturn(expected);

        // when
        BatchJobExecutionResponseDto result = batchInternalClient.runWeeklyPriceJob(year, week);

        // then
        assertThat(result).isSameAs(expected);
        UriBuilder mockUriBuilder = mock(UriBuilder.class, Answers.RETURNS_SELF);
        uriCaptor.getValue().apply(mockUriBuilder);
        verify(mockUriBuilder).queryParam("year", year);
        verify(mockUriBuilder).queryParam("week", week);
    }

    @Test
    @DisplayName("주별 범위 수집 시 POST /weekly-price/range 요청을 전달하고 startYear, startWeek, endYear, endWeek 쿼리 파라미터를 설정한다")
    @SuppressWarnings("unchecked")
    void collectWeeklyRange() {
        // given
        Integer startYear = 2025;
        Integer startWeek = 1;
        Integer endYear = 2025;
        Integer endWeek = 10;
        BatchWeeklyRangeResponseDto expected = new BatchWeeklyRangeResponseDto(
                true, List.of(1L, 2L, 3L), List.of("COMPLETED", "COMPLETED", "COMPLETED"));
        ArgumentCaptor<UnaryOperator<UriBuilder>> uriCaptor = ArgumentCaptor.forClass(UnaryOperator.class);
        given(httpTemplate.post(eq("/weekly-price/range"), eq(BatchWeeklyRangeResponseDto.class), uriCaptor.capture()))
                .willReturn(expected);

        // when
        BatchWeeklyRangeResponseDto result = batchInternalClient.collectWeeklyRange(startYear, startWeek, endYear, endWeek);

        // then
        assertThat(result).isSameAs(expected);
        UriBuilder mockUriBuilder = mock(UriBuilder.class, Answers.RETURNS_SELF);
        uriCaptor.getValue().apply(mockUriBuilder);
        verify(mockUriBuilder).queryParam("startYear", startYear);
        verify(mockUriBuilder).queryParam("startWeek", startWeek);
        verify(mockUriBuilder).queryParam("endYear", endYear);
        verify(mockUriBuilder).queryParam("endWeek", endWeek);
    }

    @Test
    @DisplayName("월별 가격 Job 실행 시 POST /monthly-price 요청을 전달하고 year, month 쿼리 파라미터를 설정한다")
    @SuppressWarnings("unchecked")
    void runMonthlyPriceJob() {
        // given
        Integer year = 2025;
        Integer month = 3;
        BatchJobExecutionResponseDto expected = new BatchJobExecutionResponseDto(
                true, 3L, "COMPLETED", null, null, "2025", null, "3");
        ArgumentCaptor<UnaryOperator<UriBuilder>> uriCaptor = ArgumentCaptor.forClass(UnaryOperator.class);
        given(httpTemplate.post(eq("/monthly-price"), eq(BatchJobExecutionResponseDto.class), uriCaptor.capture()))
                .willReturn(expected);

        // when
        BatchJobExecutionResponseDto result = batchInternalClient.runMonthlyPriceJob(year, month);

        // then
        assertThat(result).isSameAs(expected);
        UriBuilder mockUriBuilder = mock(UriBuilder.class, Answers.RETURNS_SELF);
        uriCaptor.getValue().apply(mockUriBuilder);
        verify(mockUriBuilder).queryParam("year", year);
        verify(mockUriBuilder).queryParam("month", month);
    }

    @Test
    @DisplayName("연별 가격 Job 실행 시 POST /yearly-price 요청을 전달하고 year 쿼리 파라미터를 설정한다")
    @SuppressWarnings("unchecked")
    void runYearlyPriceJob() {
        // given
        Integer year = 2025;
        BatchJobExecutionResponseDto expected = new BatchJobExecutionResponseDto(
                true, 4L, "COMPLETED", null, null, "2025", null, null);
        ArgumentCaptor<UnaryOperator<UriBuilder>> uriCaptor = ArgumentCaptor.forClass(UnaryOperator.class);
        given(httpTemplate.post(eq("/yearly-price"), eq(BatchJobExecutionResponseDto.class), uriCaptor.capture()))
                .willReturn(expected);

        // when
        BatchJobExecutionResponseDto result = batchInternalClient.runYearlyPriceJob(year);

        // then
        assertThat(result).isSameAs(expected);
        UriBuilder mockUriBuilder = mock(UriBuilder.class, Answers.RETURNS_SELF);
        uriCaptor.getValue().apply(mockUriBuilder);
        verify(mockUriBuilder).queryParam("year", year);
    }
}
