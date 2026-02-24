package com.yachaerang.batch.controller;

import com.yachaerang.batch.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
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
     * 기간 범위 데이터 수집
     * 일자를 기준으로 기간에 대하여 일별 데이터 수집
     */
    @PostMapping("/date-range")
    public ResponseEntity<Map<String, Object>> collectDateRange(
            @RequestParam(name = "startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Map<String, Object> response = new HashMap<>();
        try {
            JobExecution execution = batchJobService.collectDateRange(startDate, endDate);
            response.put("success", true);
            response.put("jobId", execution.getJobId());
            response.put("status", execution.getStatus().toString());
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정년도의 특정 week에 대한 조사
     * 
     * @param year : 대상 년도
     * @param week : 대상 주차(N주차)
     * @return
     */
    @PostMapping("/weekly-price")
    public ResponseEntity<Map<String, Object>> runWeeklyPriceJob(
            @RequestParam("year") Integer year,
            @RequestParam("week") Integer week) {
        Map<String, Object> response = new HashMap<>();
        try {
            JobExecution execution = batchJobService.runWeeklyAggregation(year, week);
            response.put("success", true);
            response.put("jobId", execution.getJobId());
            response.put("status", execution.getStatus().toString());
            response.put("year", year.toString());
            response.put("week", week.toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     */
    @PostMapping("/weekly-price/range")
    public ResponseEntity<Map<String, Object>> runWeeklyPriceJob(
            @RequestParam("startYear") Integer startYear,
            @RequestParam("startWeek") Integer startWeek,
            @RequestParam("endYear") Integer endYear,
            @RequestParam("endWeek") Integer endWeek) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<JobExecution> executionList = batchJobService.collectWeekly(startYear, startWeek, endYear, endWeek);

            // 공통 응답 데이터
            response.put("success", true);

            response.put("jobIdList", executionList.stream()
                    .map(JobExecution::getJobId)
                    .collect(Collectors.toList()));
            response.put("statusList", executionList.stream()
                    .map(JobExecution::getStatus)
                    .collect(Collectors.toList()));

            response.put("parameters", executionList.stream()
                    .map(jobExecution -> jobExecution.getJobParameters().getParameters())
                    .map(params -> params.values().stream()
                            .map(param -> param.getValue())
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /*
     * 특정 월의 월간 집계 데이터 구하기
     */
    @PostMapping("/monthly-price")
    public ResponseEntity<Map<String, Object>> runMonthlyPriceJob(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) throws JobExecutionException {

        Map<String, Object> response = new HashMap<>();
        try {
            JobExecution execution = batchJobService.runMonthlyAggregation(year, month);
            response.put("success", true);
            response.put("jobId", execution.getJobId());
            response.put("status", execution.getStatus().toString());
            response.put("year", year.toString());
            response.put("month", month.toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /*
     * 특정 년도의 연간 데이터 구하기
     */
    @PostMapping("/yearly-price")
    public ResponseEntity<Map<String, Object>> runYearlyPriceJob(@RequestParam Integer year) {

        Map<String, Object> response = new HashMap<>();
        try {
            JobExecution execution = batchJobService.runYearlyAggregation(year);
            response.put("success", true);
            response.put("jobId", execution.getJobId());
            response.put("status", execution.getStatus().toString());
            response.put("year", year.toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
