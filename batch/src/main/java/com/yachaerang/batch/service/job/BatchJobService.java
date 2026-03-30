package com.yachaerang.batch.service.job;

import com.yachaerang.batch.domain.dto.JobStartResponseDto;
import com.yachaerang.batch.exception.GeneralException;
import com.yachaerang.batch.util.WeekUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchJobService {

    private final JobLauncher asyncJobLauncher;
    private final Job dateRangePriceJob;
    private final Job dailyPriceJob;
    private final Job weeklyPriceJob;
    private final Job monthlyPriceJob;
    private final Job yearlyPriceJob;
    private final Job redisAggregationInitJob;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 단일 날짜에 대한 조회
     */
    public JobExecution runManually(LocalDate targetDate) {
        try {
            log.info("Daily Price Job 수동 시작: date={}", targetDate);
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", targetDate.format(FORMATTER))
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            return asyncJobLauncher.run(dailyPriceJob, jobParameters);
        } catch (Exception e) {
            log.error("수동 실행 실패", e);
            throw new GeneralException("Job 실행 실패", e);
        }
    }

    /**
     * 특정 기간의 데이터 수집
     * 중복 실행 정책: requestId(UUID)를 JobParameter에 포함하여 동일 날짜 범위도 독립 Job으로 처리 (중복 허용)
     */
    public JobStartResponseDto collectDateRange(LocalDate startDate, LocalDate endDate)
            throws JobExecutionException {

        String requestId = UUID.randomUUID().toString();
        log.info("기간 범위 배치 접수: {} ~ {}, requestId={}", startDate, endDate, requestId);

        JobParameters params = new JobParametersBuilder()
                .addString("startDate", startDate.format(FORMATTER))
                .addString("endDate", endDate.format(FORMATTER))
                .addString("requestId", requestId)
                .toJobParameters();

        JobExecution execution = asyncJobLauncher.run(dateRangePriceJob, params);
        log.info("기간 범위 배치 접수 완료: jobId={}, status={}", execution.getJobId(), execution.getStatus());
        return JobStartResponseDto.builder()
                .jobId(execution.getJobId())
                .status(execution.getStatus().toString())
                .message("Batch job accepted")
                .build();
    }

    /**
     * 이전 월의 일자별 데이터 전체 수집 (비동기 실행)
     * 스케줄러로 사용할 로직
     */
    public JobStartResponseDto collectPreviousMonth() throws JobExecutionException {

        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = previousMonth.atDay(1);
        LocalDate endDate = previousMonth.atEndOfMonth();

        log.info("이전의 달 배치 실행: {}-{}", previousMonth.getYear(), previousMonth.getMonthValue());

        return collectDateRange(startDate, endDate);
    }

    /**
     * 특정 주간 가격 집계 실행
     */
    public JobStartResponseDto runWeeklyAggregation(Integer year, Integer week) {

        if (week > WeekUtils.getLastIsoWeekOfYear(year)) {
            throw new IllegalArgumentException("week가 해당 년도의 주차에 존재하지 않는 주차입니다.");
        }

        LocalDate startDate = WeekUtils.getWeekStartDate(year, week);
        LocalDate endDate = WeekUtils.getWeekEndDate(year, week);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        if (!endDate.isBefore(yesterday)) {
            throw new IllegalArgumentException("아직 완료되지 않은 주입니다.");
        }

        log.info("주간 집계 접수 - 입력: {}년 {}주차, 기간: {} ~ {}", year, week, startDate, endDate);
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("year", year.toString())
                    .addString("week", week.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = asyncJobLauncher.run(weeklyPriceJob, params);
            log.info("주간 집계 접수 완료: jobId={}, status={}", execution.getJobId(), execution.getStatus());
            return JobStartResponseDto.builder()
                    .jobId(execution.getJobId())
                    .status(execution.getStatus().toString())
                    .build();
        } catch (Exception e) {
            log.error("주간 가격 집계 Job 실행 실패", e);
            throw new RuntimeException("Job 실행 실패", e);
        }
    }


    /**
     * 특정 기간의 주간 데이터를 수동으로 수집
     *
     * @param startYear
     * @param startWeek
     * @param endYear
     * @param endWeek
     * @return
     */
    public List<JobStartResponseDto> collectWeekly(
            Integer startYear, Integer startWeek, Integer endYear, Integer endWeek
    ) {
        if (startYear > endYear) {
            throw new IllegalArgumentException("날짜 설정을 똑바로 해주세요.");
        }
        if (startWeek < 1 || startWeek > 53 || endWeek < 1 || endWeek > 53) {
            throw new IllegalArgumentException("주차는 1~52 또는 53만 허용합니다.");
        }

        log.info("주간 집계 범위 접수 시작 - {}년 {}주 ~ {}년 {}주",
                startYear, startWeek, endYear, endWeek);

        List<JobStartResponseDto> resultList = new ArrayList<>();

        try {
            // 같은 연도일 때
            if (startYear.intValue() == endYear.intValue()) {
                for (int week = startWeek; week <= endWeek; week++) {
                    resultList.add(runWeeklyAggregation(startYear, week));
                }
            }
            // 연도가 다를 때
            else {
                // 시작년도의 startWeek ~ 마지막 주까지
                int lastWeekOfStartYear = WeekUtils.getLastIsoWeekOfYear(startYear);
                for (int week = startWeek; week <= lastWeekOfStartYear; week++) {
                    resultList.add(runWeeklyAggregation(startYear, week));
                }

                // 중간년도(1년 넘게 차이난다면)
                for (int year = startYear + 1; year < endYear; year++) {
                    int lastWeekOfYear = WeekUtils.getLastIsoWeekOfYear(year);
                    for (int week = 1; week <= lastWeekOfYear; week++) {
                        resultList.add(runWeeklyAggregation(year, week));
                    }
                }

                // 종료년도의 1주차부터 endWeek까지
                for (int week = 1; week <= endWeek; week++) {
                    resultList.add(runWeeklyAggregation(endYear, week));
                }
            }

            log.info("주간 집계 범위 접수 완료 - 총 {}개 Job 접수", resultList.size());
            return resultList;

        } catch (Exception e) {
            log.error("주간 집계 범위 실행 중 예외 발생", e);
            throw new RuntimeException("주간 집계 범위 실행 실패", e);
        }
    }


    /**
     * 특정 기간의 월간 가격 집계 실행
     */
    public JobStartResponseDto runMonthlyAggregation(Integer year, Integer month) throws JobExecutionException {
        if (year == null || month == null) {
            throw new GeneralException("year와 month는 필수입니다.");
        }
        if (month < 1 || month > 12) {
            throw new GeneralException("month는 1~12 사이여야합니다.");
        }
        YearMonth targetMonth = YearMonth.of(year, month);
        YearMonth currentMonth = YearMonth.now();
        if (!targetMonth.isBefore(currentMonth)) {
            throw new GeneralException(
                    String.format("아직 완료되지 않은 달입니다. 대상: %d년 %d월", year, month)
            );
        }

        log.info("월간 집계 접수 - 대상: {}년 {}월", year, month);

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("year", year.toString())
                    .addString("month", month.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = asyncJobLauncher.run(monthlyPriceJob, params);
            log.info("월간 집계 접수 완료: jobId={}, status={}", execution.getJobId(), execution.getStatus());
            return JobStartResponseDto.builder()
                    .jobId(execution.getJobId())
                    .status(execution.getStatus().toString())
                    .build();
        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("이미 실행 중인 Job입니다.");
            throw new GeneralException("이미 실행 중인 Job입니다.", e);
        } catch (Exception e) {
            log.error("월간 가격 집계 Job 실행 실패", e);
            throw new GeneralException("Job 실행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Redis 초기화 Job 실행
     */
    public JobStartResponseDto runRedisInit() {
        log.info("Redis 집계 초기화 Job 접수");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = asyncJobLauncher.run(redisAggregationInitJob, params);
            log.info("Redis 초기화 Job 접수 완료: jobId={}, status={}", execution.getJobId(), execution.getStatus());
            return JobStartResponseDto.builder()
                    .jobId(execution.getJobId())
                    .status(execution.getStatus().toString())
                    .message("Redis aggregation init job accepted")
                    .build();
        } catch (Exception e) {
            log.error("Redis 초기화 Job 실행 실패", e);
            throw new RuntimeException("Job 실행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 연도의 연간 가격 집계 실행
     */
    public JobStartResponseDto runYearlyAggregation(Integer year) {
        if (year == null) {
            throw new IllegalArgumentException("year는 필수입니다.");
        }

        int currentYear = LocalDate.now().getYear();
        if (year >= currentYear) {
            throw new IllegalArgumentException(
                    String.format("아직 완료되지 않은 연도입니다. 대상: %d년", year)
            );
        }

        log.info("연간 집계 접수 - 대상: {}년", year);

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("year", year.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = asyncJobLauncher.run(yearlyPriceJob, params);
            log.info("연간 집계 접수 완료: jobId={}, status={}", execution.getJobId(), execution.getStatus());
            return JobStartResponseDto.builder()
                    .jobId(execution.getJobId())
                    .status(execution.getStatus().toString())
                    .build();
        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("이미 실행 중인 Job입니다.");
            throw new RuntimeException("이미 실행 중인 Job입니다.", e);
        } catch (Exception e) {
            log.error("연간 가격 집계 Job 실행 실패", e);
            throw new RuntimeException("Job 실행 실패: " + e.getMessage(), e);
        }
    }
}
