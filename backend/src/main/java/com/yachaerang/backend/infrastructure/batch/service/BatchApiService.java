package com.yachaerang.backend.infrastructure.batch.service;

import com.yachaerang.backend.infrastructure.batch.client.BatchInternalClient;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchDailyResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStartResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStatusResponseDto;
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

    public BatchJobStartResponseDto collectDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("[BatchApi] collectDateRange startDate={} endDate={}", startDate, endDate);
        BatchJobStartResponseDto response = batchInternalClient.collectDateRange(startDate, endDate);
        log.info("[BatchApi] collectDateRange accepted: jobId={}", response.getJobId());
        return response;
    }

    public BatchJobStatusResponseDto getJobStatus(Long jobId) {
        log.info("[BatchApi] getJobStatus jobId={}", jobId);
        return batchInternalClient.getJobStatus(jobId);
    }

    public BatchJobStartResponseDto runWeeklyPriceJob(Integer year, Integer week) {
        log.info("[BatchApi] runWeeklyPriceJob year={} week={}", year, week);
        return batchInternalClient.runWeeklyPriceJob(year, week);
    }

    public BatchWeeklyRangeResponseDto collectWeeklyRange(Integer startYear, Integer startWeek,
                                                          Integer endYear, Integer endWeek) {
        log.info("[BatchApi] collectWeeklyRange startYear={} startWeek={} endYear={} endWeek={}",
                startYear, startWeek, endYear, endWeek);
        return batchInternalClient.collectWeeklyRange(startYear, startWeek, endYear, endWeek);
    }

    public BatchJobStartResponseDto runMonthlyPriceJob(Integer year, Integer month) {
        log.info("[BatchApi] runMonthlyPriceJob year={} month={}", year, month);
        return batchInternalClient.runMonthlyPriceJob(year, month);
    }

    public BatchJobStartResponseDto runYearlyPriceJob(Integer year) {
        log.info("[BatchApi] runYearlyPriceJob year={}", year);
        return batchInternalClient.runYearlyPriceJob(year);
    }
}
