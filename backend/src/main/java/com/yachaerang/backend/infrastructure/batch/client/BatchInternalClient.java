package com.yachaerang.backend.infrastructure.batch.client;

import com.yachaerang.backend.infrastructure.batch.dto.response.BatchDailyResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStartResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStatusResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchWeeklyRangeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BatchInternalClient {

    private final BatchHttpTemplate httpTemplate;

    public BatchDailyResponseDto runDailyJob(LocalDate targetDate) {
        return httpTemplate.get("/daily", BatchDailyResponseDto.class,
                b -> b.queryParam("targetDate", targetDate));
    }

    public BatchJobStartResponseDto collectDateRange(LocalDate startDate, LocalDate endDate) {
        return httpTemplate.post("/date-range", BatchJobStartResponseDto.class,
                b -> b.queryParam("startDate", startDate)
                      .queryParam("endDate", endDate));
    }

    public BatchJobStatusResponseDto getJobStatus(Long jobId) {
        return httpTemplate.get("/job-status/" + jobId, BatchJobStatusResponseDto.class, b -> b);
    }

    public BatchJobStartResponseDto runWeeklyPriceJob(Integer year, Integer week) {
        return httpTemplate.post("/weekly-price", BatchJobStartResponseDto.class,
                b -> b.queryParam("year", year)
                      .queryParam("week", week));
    }

    public BatchWeeklyRangeResponseDto collectWeeklyRange(Integer startYear, Integer startWeek,
                                                           Integer endYear, Integer endWeek) {
        return httpTemplate.post("/weekly-price/range", BatchWeeklyRangeResponseDto.class,
                b -> b.queryParam("startYear", startYear)
                      .queryParam("startWeek", startWeek)
                      .queryParam("endYear", endYear)
                      .queryParam("endWeek", endWeek));
    }

    public BatchJobStartResponseDto runMonthlyPriceJob(Integer year, Integer month) {
        return httpTemplate.post("/monthly-price", BatchJobStartResponseDto.class,
                b -> b.queryParam("year", year)
                      .queryParam("month", month));
    }

    public BatchJobStartResponseDto runYearlyPriceJob(Integer year) {
        return httpTemplate.post("/yearly-price", BatchJobStartResponseDto.class,
                b -> b.queryParam("year", year));
    }

    public BatchJobStartResponseDto runRedisInit() {
        return httpTemplate.post("/redis-init", BatchJobStartResponseDto.class, b -> b);
    }
}
