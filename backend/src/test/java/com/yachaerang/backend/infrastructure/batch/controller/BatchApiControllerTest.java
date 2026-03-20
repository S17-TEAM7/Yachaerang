package com.yachaerang.backend.infrastructure.batch.controller;

import com.yachaerang.backend.global.response.ResponseWrappingAdvice;
import com.yachaerang.backend.global.util.RestDocsSupport;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchDailyResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStartResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStatusResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchWeeklyRangeResponseDto;
import com.yachaerang.backend.infrastructure.batch.service.BatchApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(ResponseWrappingAdvice.class)
class BatchApiControllerTest extends RestDocsSupport {

    private final BatchApiService batchApiService = mock(BatchApiService.class);

    @Override
    protected Object[] initControllerAdvices() {
        return new Object[]{
                new ResponseWrappingAdvice()
        };
    }

    @Override
    protected Object initController() {
        return new BatchApiController(batchApiService);
    }

    private static final FieldDescriptor[] ENVELOPE_COMMON = new FieldDescriptor[]{
            fieldWithPath("httpStatus").type(STRING).description("HTTP 상태 코드"),
            fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
            fieldWithPath("code").type(STRING).description("응답 코드"),
            fieldWithPath("message").type(STRING).description("응답 메시지")
    };

    @Test
    @DisplayName("[GET] /api/batch/daily - 일별 가격 수집 Job 실행")
    void runDailyJob() throws Exception {
        // given
        LocalDate targetDate = LocalDate.of(2025, 3, 18);
        given(batchApiService.runDailyJob(targetDate))
                .willReturn(new BatchDailyResponseDto("COMPLETED", targetDate.toString()));

        // when & then
        mockMvc.perform(get("/api/batch/daily")
                        .param("targetDate", "2025-03-18"))
                .andExpect(status().isOk())
                .andDo(doc(
                        "batch-daily",
                        requestHeaders(),
                        queryParameters(
                                parameterWithName("targetDate").description("수집 대상 날짜 (yyyy-MM-dd)")
                        ),
                        responseFields(ENVELOPE_COMMON).and(
                                fieldWithPath("data.status").type(STRING).description("Job 실행 상태"),
                                fieldWithPath("data.targetDate").type(STRING).description("수집 대상 날짜")
                        )
                ));
    }

    @Test
    @DisplayName("[POST] /api/batch/date-range - 기간 범위 일별 데이터 수집")
    void collectDateRange() throws Exception {
        // given
        LocalDate startDate = LocalDate.of(2025, 3, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 18);
        given(batchApiService.collectDateRange(startDate, endDate))
                .willReturn(new BatchJobStartResponseDto(1L, "STARTING", "Batch job accepted"));

        // when & then
        mockMvc.perform(post("/api/batch/date-range")
                        .queryParam("startDate", "2025-03-01")
                        .queryParam("endDate", "2025-03-18"))
                .andExpect(status().isOk())
                .andDo(doc(
                        "batch-date-range",
                        requestHeaders(),
                        queryParameters(
                                parameterWithName("startDate").description("시작 날짜 (yyyy-MM-dd)"),
                                parameterWithName("endDate").description("종료 날짜 (yyyy-MM-dd)")
                        ),
                        responseFields(ENVELOPE_COMMON).and(
                                fieldWithPath("data.jobId").type(NUMBER).description("비동기 Job ID"),
                                fieldWithPath("data.status").type(STRING).description("Job 접수 상태 (STARTING)"),
                                fieldWithPath("data.message").type(STRING).description("접수 결과 메시지")
                        )
                ));
    }

    @Test
    @DisplayName("[GET] /api/batch/job-status/{jobId} - Job 실행 상태 조회")
    void getJobStatus() throws Exception {
        // given
        LocalDateTime startTime = LocalDateTime.of(2025, 3, 18, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 3, 18, 10, 5, 0);
        given(batchApiService.getJobStatus(1L))
                .willReturn(new BatchJobStatusResponseDto(
                        1L, "dateRangePriceJob", "COMPLETED", "COMPLETED",
                        startTime, endTime, "Job 완료", null));

        // when & then
        mockMvc.perform(get("/api/batch/job-status/{jobId}", 1L))
                .andExpect(status().isOk())
                .andDo(doc(
                        "batch-job-status",
                        requestHeaders(),
                        pathParameters(
                                parameterWithName("jobId").description("조회할 Job ID")
                        ),
                        responseFields(ENVELOPE_COMMON).and(
                                fieldWithPath("data.jobId").type(NUMBER).description("Job ID"),
                                fieldWithPath("data.jobName").type(STRING).description("Job 이름"),
                                fieldWithPath("data.status").type(STRING).description("Job 실행 상태"),
                                fieldWithPath("data.exitStatus").type(STRING).description("Job 종료 코드"),
                                fieldWithPath("data.startTime").type(STRING).description("Job 시작 시간"),
                                fieldWithPath("data.endTime").type(STRING).description("Job 종료 시간"),
                                fieldWithPath("data.message").type(STRING).description("상태 메시지")
                        )
                ));
    }

    @Test
    @DisplayName("[POST] /api/batch/weekly-price - 특정 년도/주차 주간 집계")
    void runWeeklyPriceJob() throws Exception {
        // given
        given(batchApiService.runWeeklyPriceJob(2025, 11))
                .willReturn(new BatchJobStartResponseDto(1L, "STARTING", null));

        // when & then
        mockMvc.perform(post("/api/batch/weekly-price")
                        .queryParam("year", "2025")
                        .queryParam("week", "11"))
                .andExpect(status().isOk())
                .andDo(doc(
                        "batch-weekly-price",
                        requestHeaders(),
                        queryParameters(
                                parameterWithName("year").description("년도"),
                                parameterWithName("week").description("주차")
                        ),
                        responseFields(ENVELOPE_COMMON).and(
                                fieldWithPath("data.jobId").type(NUMBER).description("비동기 Job ID"),
                                fieldWithPath("data.status").type(STRING).description("Job 접수 상태 (STARTING)")
                        )
                ));
    }

    @Test
    @DisplayName("[POST] /api/batch/weekly-price/range - 주간 범위 집계")
    void collectWeeklyRange() throws Exception {
        // given
        given(batchApiService.collectWeeklyRange(2025, 1, 2025, 11))
                .willReturn(new BatchWeeklyRangeResponseDto(
                        null,
                        List.of(1L, 2L),
                        List.of("STARTING", "STARTING")));

        // when & then
        mockMvc.perform(post("/api/batch/weekly-price/range")
                        .queryParam("startYear", "2025")
                        .queryParam("startWeek", "1")
                        .queryParam("endYear", "2025")
                        .queryParam("endWeek", "11"))
                .andExpect(status().isOk())
                .andDo(doc(
                        "batch-weekly-price-range",
                        requestHeaders(),
                        queryParameters(
                                parameterWithName("startYear").description("시작 년도"),
                                parameterWithName("startWeek").description("시작 주차"),
                                parameterWithName("endYear").description("종료 년도"),
                                parameterWithName("endWeek").description("종료 주차")
                        ),
                        responseFields(ENVELOPE_COMMON).and(
                                fieldWithPath("data.jobIdList").type(ARRAY).description("Job 실행 ID 목록"),
                                fieldWithPath("data.statusList").type(ARRAY).description("Job 접수 상태 목록")
                        )
                ));
    }

    @Test
    @DisplayName("[POST] /api/batch/monthly-price - 특정 년도/월 월간 집계")
    void runMonthlyPriceJob() throws Exception {
        // given
        given(batchApiService.runMonthlyPriceJob(2025, 3))
                .willReturn(new BatchJobStartResponseDto(1L, "STARTING", null));

        // when & then
        mockMvc.perform(post("/api/batch/monthly-price")
                        .queryParam("year", "2025")
                        .queryParam("month", "3"))
                .andExpect(status().isOk())
                .andDo(doc(
                        "batch-monthly-price",
                        requestHeaders(),
                        queryParameters(
                                parameterWithName("year").description("년도"),
                                parameterWithName("month").description("월")
                        ),
                        responseFields(ENVELOPE_COMMON).and(
                                fieldWithPath("data.jobId").type(NUMBER).description("비동기 Job ID"),
                                fieldWithPath("data.status").type(STRING).description("Job 접수 상태 (STARTING)")
                        )
                ));
    }

    @Test
    @DisplayName("[POST] /api/batch/yearly-price - 특정 년도 연간 집계")
    void runYearlyPriceJob() throws Exception {
        // given
        given(batchApiService.runYearlyPriceJob(2025))
                .willReturn(new BatchJobStartResponseDto(1L, "STARTING", null));

        // when & then
        mockMvc.perform(post("/api/batch/yearly-price")
                        .queryParam("year", "2025"))
                .andExpect(status().isOk())
                .andDo(doc(
                        "batch-yearly-price",
                        requestHeaders(),
                        queryParameters(
                                parameterWithName("year").description("년도")
                        ),
                        responseFields(ENVELOPE_COMMON).and(
                                fieldWithPath("data.jobId").type(NUMBER).description("비동기 Job ID"),
                                fieldWithPath("data.status").type(STRING).description("Job 접수 상태 (STARTING)")
                        )
                ));
    }
}
