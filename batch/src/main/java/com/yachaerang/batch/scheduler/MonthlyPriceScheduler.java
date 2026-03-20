package com.yachaerang.batch.scheduler;

import com.yachaerang.batch.domain.dto.JobStartResponseDto;
import com.yachaerang.batch.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class   MonthlyPriceScheduler {

    private final BatchJobService batchJobService;

    /**
     * 매월 1일 새벽 2시에 전월의 일간 데이터 수집
     */
    @Scheduled(cron = "0 0 2 1 * *")
    public void collectPreviousMonthData() {
        log.info("========================================");
        log.info("지난 달의 일간 배치 스케줄러 시작: {}", LocalDateTime.now());
        log.info("========================================");

        try {
            String[] categories = {"100", "200", "300", "400", "500", "600"};
            for (String category : categories) {
                try {
                    JobStartResponseDto response = batchJobService.collectPreviousMonth();
                    log.info("카테고리 {} 배치 접수 완료: jobId={}, status={}", category, response.getJobId(), response.getStatus());
                } catch (JobExecutionException e) {
                    log.error("카테고리 {} 수집 실패: {}", category, e.getMessage());
                }
            }
            log.info("지난 달의 일간 배치 스케줄러 완료");
        } catch (Exception e) {
            log.error("지난 달의 일간 배치 스케줄러 실패", e);
        }
    }

    /**
     * 매월 1일 새벽 3시에 전월의 월간 데이터 수집
     */
    @Scheduled(cron = "0 0 3 1 * *")
    public void runLastMonthAggregation() {
        log.info("========================================");
        log.info("월간 배치 스케줄러 시작: {}", YearMonth.now().minusMonths(1));
        log.info("========================================");

        try {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            batchJobService.runMonthlyAggregation(lastMonth.getYear(), lastMonth.getMonthValue());
            log.info("월간 배치 스케줄러 완료");
        } catch (Exception e) {
            log.error("월간 배치 스케줄러 실패", e);
        }
    }
}
