package com.yachaerang.backend.infrastructure.batch.controller;

import com.yachaerang.backend.infrastructure.batch.dto.response.BatchDailyResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStartResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchJobStatusResponseDto;
import com.yachaerang.backend.infrastructure.batch.dto.response.BatchWeeklyRangeResponseDto;
import com.yachaerang.backend.infrastructure.batch.service.BatchApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchApiController {

    private final BatchApiService batchApiService;

    /**
     * 수동으로 일별 가격 수집 Job 실행
     */
    @GetMapping("/daily")
    public BatchDailyResponseDto runDailyJob(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate) {
        return batchApiService.runDailyJob(targetDate);
    }

    /**
     * 기간 범위 일별 데이터 수집
     */
    @PostMapping("/date-range")
    public BatchJobStartResponseDto collectDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return batchApiService.collectDateRange(startDate, endDate);
    }

    /**
     * Job 실행 상태 조회
     */
    @GetMapping("/job-status/{jobId}")
    public BatchJobStatusResponseDto getJobStatus(@PathVariable Long jobId) {
        return batchApiService.getJobStatus(jobId);
    }

    /**
     * 특정 년도/주차 주간 집계
     */
    @PostMapping("/weekly-price")
    public BatchJobStartResponseDto runWeeklyPriceJob(
            @RequestParam Integer year,
            @RequestParam Integer week) {
        return batchApiService.runWeeklyPriceJob(year, week);
    }

    /**
     * 주간 범위 집계
     */
    @PostMapping("/weekly-price/range")
    public BatchWeeklyRangeResponseDto collectWeeklyRange(
            @RequestParam Integer startYear,
            @RequestParam Integer startWeek,
            @RequestParam Integer endYear,
            @RequestParam Integer endWeek) {
        return batchApiService.collectWeeklyRange(startYear, startWeek, endYear, endWeek);
    }

    /**
     * 특정 년도/월 월간 집계
     */
    @PostMapping("/monthly-price")
    public BatchJobStartResponseDto runMonthlyPriceJob(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return batchApiService.runMonthlyPriceJob(year, month);
    }

    /**
     * 특정 년도 연간 집계
     */
    @PostMapping("/yearly-price")
    public BatchJobStartResponseDto runYearlyPriceJob(
            @RequestParam Integer year) {
        return batchApiService.runYearlyPriceJob(year);
    }

    /**
     * Redis 집계 초기화
     */
    @PostMapping("/redis-init")
    public BatchJobStartResponseDto runRedisInit() {
        return batchApiService.runRedisInit();
    }
}
