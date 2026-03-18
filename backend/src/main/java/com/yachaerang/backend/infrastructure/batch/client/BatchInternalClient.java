package com.yachaerang.backend.infrastructure.batch.client;

import com.yachaerang.backend.infrastructure.batch.dto.response.BatchDailyResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobExecutionResponseDto;
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

    public BatchJobExecutionResponseDto collectDateRange(LocalDate startDate, LocalDate endDate) {
        return httpTemplate.post("/date-range", BatchJobExecutionResponseDto.class,
                b -> b.queryParam("startDate", startDate)
                      .queryParam("endDate", endDate));
    }

    public BatchJobExecutionResponseDto runWeeklyPriceJob(Integer year, Integer week) {
        return httpTemplate.post("/weekly-price", BatchJobExecutionResponseDto.class,
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

    public BatchJobExecutionResponseDto runMonthlyPriceJob(Integer year, Integer month) {
        return httpTemplate.post("/monthly-price", BatchJobExecutionResponseDto.class,
                b -> b.queryParam("year", year)
                      .queryParam("month", month));
    }

    public BatchJobExecutionResponseDto runYearlyPriceJob(Integer year) {
        return httpTemplate.post("/yearly-price", BatchJobExecutionResponseDto.class,
                b -> b.queryParam("year", year));
    }
}
