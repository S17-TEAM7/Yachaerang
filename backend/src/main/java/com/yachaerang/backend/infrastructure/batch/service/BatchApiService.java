package com.yachaerang.backend.infrastructure.batch.service;

import com.yachaerang.backend.infrastructure.batch.client.BatchInternalClient;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchDailyResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobExecutionResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchWeeklyRangeResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchApiService {

    private final BatchInternalClient batchInternalClient;

    public BatchDailyResponseDto runDailyJob(LocalDate targetDate) {
        log.info("[BatchApi] runDailyJob targetDate={}", targetDate);
        return batchInternalClient.runDailyJob(targetDate);
    }

    public BatchJobExecutionResponseDto collectDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("[BatchApi] collectDateRange startDate={} endDate={}", startDate, endDate);
        return batchInternalClient.collectDateRange(startDate, endDate);
    }

    public BatchJobExecutionResponseDto runWeeklyPriceJob(Integer year, Integer week) {
        log.info("[BatchApi] runWeeklyPriceJob year={} week={}", year, week);
        return batchInternalClient.runWeeklyPriceJob(year, week);
    }

    public BatchWeeklyRangeResponseDto collectWeeklyRange(Integer startYear, Integer startWeek,
                                                          Integer endYear, Integer endWeek) {
        log.info("[BatchApi] collectWeeklyRange startYear={} startWeek={} endYear={} endWeek={}",
                startYear, startWeek, endYear, endWeek);
        return batchInternalClient.collectWeeklyRange(startYear, startWeek, endYear, endWeek);
    }

    public BatchJobExecutionResponseDto runMonthlyPriceJob(Integer year, Integer month) {
        log.info("[BatchApi] runMonthlyPriceJob year={} month={}", year, month);
        return batchInternalClient.runMonthlyPriceJob(year, month);
    }

    public BatchJobExecutionResponseDto runYearlyPriceJob(Integer year) {
        log.info("[BatchApi] runYearlyPriceJob year={}", year);
        return batchInternalClient.runYearlyPriceJob(year);
    }
}
