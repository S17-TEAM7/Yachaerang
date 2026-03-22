package com.yachaerang.backend.infrastructure.batch.service;

import com.yachaerang.backend.global.exception.BatchException;
import com.yachaerang.backend.global.response.ErrorCode;
import com.yachaerang.backend.infrastructure.batch.client.BatchInternalClient;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchDailyResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStartResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStatusResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchWeeklyRangeResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchApiServiceTest {

    @Mock
    private BatchInternalClient batchInternalClient;

    @InjectMocks
    private BatchApiService batchApiService;

    private static final LocalDate TARGET_DATE = LocalDate.of(2025, 3, 18);
    private static final LocalDate START_DATE  = LocalDate.of(2025, 3, 1);
    private static final LocalDate END_DATE    = LocalDate.of(2025, 3, 18);

    private BatchDailyResponseDto dailyResponse() {
        return new BatchDailyResponseDto("COMPLETED", TARGET_DATE.toString());
    }

    private BatchJobStartResponseDto startResponse() {
        return new BatchJobStartResponseDto(1L, "STARTING", "Batch job accepted");
    }

    private BatchWeeklyRangeResponseDto weeklyRangeResponse() {
        return new BatchWeeklyRangeResponseDto(null, List.of(1L, 2L), List.of("STARTING", "STARTING"));
    }

    @Test
    @DisplayName("일별 Job 실행 성공")
    void 일별_Job_실행_성공() {
        // given
        when(batchInternalClient.runDailyJob(TARGET_DATE)).thenReturn(dailyResponse());

        // when
        BatchDailyResponseDto result = batchApiService.runDailyJob(TARGET_DATE);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTargetDate()).isEqualTo(TARGET_DATE.toString());
        verify(batchInternalClient).runDailyJob(TARGET_DATE);
    }

    @Test
    @DisplayName("일별 Job 실행 중 Batch 서버 오류 시 BatchException 전파")
    void 일별_Job_실행_Batch_서버_오류() {
        // given
        when(batchInternalClient.runDailyJob(TARGET_DATE))
                .thenThrow(BatchException.of(ErrorCode.BATCH_CALL_FAILED));

        // when & then
        assertThatThrownBy(() -> batchApiService.runDailyJob(TARGET_DATE))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_CALL_FAILED);
        verify(batchInternalClient).runDailyJob(TARGET_DATE);
    }

    @Test
    @DisplayName("기간 범위 데이터 수집 성공")
    void 기간_범위_데이터_수집_성공() {
        // given
        when(batchInternalClient.collectDateRange(START_DATE, END_DATE)).thenReturn(startResponse());

        // when
        BatchJobStartResponseDto result = batchApiService.collectDateRange(START_DATE, END_DATE);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("STARTING");
        verify(batchInternalClient).collectDateRange(START_DATE, END_DATE);
    }

    @Test
    @DisplayName("기간 범위 데이터 수집 중 타임아웃 시 BatchException 전파")
    void 기간_범위_데이터_수집_타임아웃() {
        // given
        when(batchInternalClient.collectDateRange(START_DATE, END_DATE))
                .thenThrow(BatchException.of(ErrorCode.BATCH_TIMEOUT));

        // when & then
        assertThatThrownBy(() -> batchApiService.collectDateRange(START_DATE, END_DATE))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_TIMEOUT);
    }

    @Test
    @DisplayName("Job 상태 조회 성공")
    void Job_상태_조회_성공() {
        // given
        BatchJobStatusResponseDto expected = new BatchJobStatusResponseDto(
                1L, "dateRangePriceJob", "COMPLETED", "COMPLETED",
                LocalDateTime.of(2025, 3, 18, 10, 0), LocalDateTime.of(2025, 3, 18, 10, 5),
                "Job 완료", null);
        when(batchInternalClient.getJobStatus(1L)).thenReturn(expected);

        // when
        BatchJobStatusResponseDto result = batchApiService.getJobStatus(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(batchInternalClient).getJobStatus(1L);
    }

    @Test
    @DisplayName("존재하지 않는 Job 조회 시 BatchException 전파")
    void Job_상태_조회_없는_Job() {
        // given
        when(batchInternalClient.getJobStatus(999L))
                .thenThrow(BatchException.of(ErrorCode.BATCH_JOB_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> batchApiService.getJobStatus(999L))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_JOB_NOT_FOUND);
    }

    @Test
    @DisplayName("주간 가격 집계 성공")
    void 주간_가격_집계_성공() {
        // given
        when(batchInternalClient.runWeeklyPriceJob(2025, 11)).thenReturn(startResponse());

        // when
        BatchJobStartResponseDto result = batchApiService.runWeeklyPriceJob(2025, 11);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("STARTING");
        verify(batchInternalClient).runWeeklyPriceJob(2025, 11);
    }

    @Test
    @DisplayName("주간 범위 집계 성공")
    void 주간_범위_집계_성공() {
        // given
        when(batchInternalClient.collectWeeklyRange(2025, 1, 2025, 11))
                .thenReturn(weeklyRangeResponse());

        // when
        BatchWeeklyRangeResponseDto result = batchApiService.collectWeeklyRange(2025, 1, 2025, 11);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getJobIdList()).hasSize(2);
        assertThat(result.getStatusList()).containsOnly("STARTING");
        verify(batchInternalClient).collectWeeklyRange(2025, 1, 2025, 11);
    }

    @Test
    @DisplayName("주간 범위 집계 중 Batch 서버 연결 실패 시 BatchException 전파")
    void 주간_범위_집계_서버_연결_실패() {
        // given
        when(batchInternalClient.collectWeeklyRange(2025, 1, 2025, 11))
                .thenThrow(BatchException.of(ErrorCode.BATCH_CONNECTION_FAILED));

        // when & then
        assertThatThrownBy(() -> batchApiService.collectWeeklyRange(2025, 1, 2025, 11))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_CONNECTION_FAILED);
    }

    @Test
    @DisplayName("월간 가격 집계 성공")
    void 월간_가격_집계_성공() {
        // given
        when(batchInternalClient.runMonthlyPriceJob(2025, 3)).thenReturn(startResponse());

        // when
        BatchJobStartResponseDto result = batchApiService.runMonthlyPriceJob(2025, 3);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("STARTING");
        verify(batchInternalClient).runMonthlyPriceJob(2025, 3);
    }

    @Test
    @DisplayName("연간 가격 집계 성공")
    void 연간_가격_집계_성공() {
        // given
        when(batchInternalClient.runYearlyPriceJob(2025)).thenReturn(startResponse());

        // when
        BatchJobStartResponseDto result = batchApiService.runYearlyPriceJob(2025);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("STARTING");
        verify(batchInternalClient).runYearlyPriceJob(2025);
    }

    @Test
    @DisplayName("연간 가격 집계 중 Batch 서버 오류 시 BatchException 전파")
    void 연간_가격_집계_Batch_서버_오류() {
        // given
        when(batchInternalClient.runYearlyPriceJob(2025))
                .thenThrow(BatchException.of(ErrorCode.BATCH_CALL_FAILED));

        // when & then
        assertThatThrownBy(() -> batchApiService.runYearlyPriceJob(2025))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_CALL_FAILED);
    }
}
