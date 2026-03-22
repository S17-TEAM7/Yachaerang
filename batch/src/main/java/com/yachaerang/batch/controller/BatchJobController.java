package com.yachaerang.batch.controller;

import com.yachaerang.batch.domain.dto.JobStartResponseDto;
import com.yachaerang.batch.domain.dto.JobStatusResponseDto;
import com.yachaerang.batch.exception.GeneralException;
import com.yachaerang.batch.service.BatchJobService;
import com.yachaerang.batch.service.JobStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class BatchJobController {

    private final BatchJobService batchJobService;
    private final JobStatusService jobStatusService;

    /**
     * 수동으로 일별 가격 수집 Job 실행
     */
    @GetMapping("/daily")
    public ResponseEntity<Map<String, String>> runDailyJob(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate) {

        batchJobService.runManually(targetDate);

        return ResponseEntity.ok(Map.of(
                "status", "Job 실행 완료",
                "targetDate", targetDate.toString()));
    }

    /**
     * Job 실행 상태 조회
     */
    @GetMapping("/job-status/{jobId}")
    public ResponseEntity<JobStatusResponseDto> getJobStatus(@PathVariable Long jobId) {
        try {
            JobStatusResponseDto response = jobStatusService.getJobStatus(jobId);
            return ResponseEntity.ok(response);
        } catch (GeneralException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 기간 범위 데이터 수집
     * jobId를 즉시 반환하며, 실제 실행은 백그라운드
     */
    @PostMapping("/date-range")
    public ResponseEntity<JobStartResponseDto> collectDateRange(
            @RequestParam(name = "startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        try {
            JobStartResponseDto response = batchJobService.collectDateRange(startDate, endDate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(
                            JobStartResponseDto.builder()
                                    .jobId(null)
                                    .status("FAILED")
                                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * 특정년도의 특정 week 주간 집계
     */
    @PostMapping("/weekly-price")
    public ResponseEntity<JobStartResponseDto> runWeeklyPriceJob(
            @RequestParam("year") Integer year,
            @RequestParam("week") Integer week) {
        try {
            JobStartResponseDto response = batchJobService.runWeeklyAggregation(year, week);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(JobStartResponseDto.builder().status("FAILED").message(e.getMessage()).build());
        }
    }

    /**
     * 주간 범위 집계
     */
    @PostMapping("/weekly-price/range")
    public ResponseEntity<Map<String, Object>> collectWeeklyRange(
            @RequestParam("startYear") Integer startYear,
            @RequestParam("startWeek") Integer startWeek,
            @RequestParam("endYear") Integer endYear,
            @RequestParam("endWeek") Integer endWeek) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<JobStartResponseDto> resultList = batchJobService.collectWeekly(startYear, startWeek, endYear, endWeek);
            response.put("jobIdList", resultList.stream()
                    .map(JobStartResponseDto::getJobId)
                    .collect(Collectors.toList()));
            response.put("statusList", resultList.stream()
                    .map(JobStartResponseDto::getStatus)
                    .collect(Collectors.toList()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 월의 월간 집계
     */
    @PostMapping("/monthly-price")
    public ResponseEntity<JobStartResponseDto> runMonthlyPriceJob(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) throws JobExecutionException {
        try {
            JobStartResponseDto response = batchJobService.runMonthlyAggregation(year, month);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(JobStartResponseDto.builder().status("FAILED").message(e.getMessage()).build());
        }
    }

    /**
     * 특정 년도의 연간 집계
     */
    @PostMapping("/yearly-price")
    public ResponseEntity<JobStartResponseDto> runYearlyPriceJob(@RequestParam Integer year) {
        try {
            JobStartResponseDto response = batchJobService.runYearlyAggregation(year);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(JobStartResponseDto.builder().status("FAILED").message(e.getMessage()).build());
        }
    }
}
