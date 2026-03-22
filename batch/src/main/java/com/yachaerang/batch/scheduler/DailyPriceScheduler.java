package com.yachaerang.batch.scheduler;

import com.yachaerang.batch.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPriceScheduler {

    private final BatchJobService batchJobService;

    /**
     * 매일 자정에 어제 날짜 데이터 수집
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void runDailyPriceJob() {
        log.info("========================================");
        log.info("일간 배치 스케줄러 시작: {}", LocalDate.now());
        log.info("========================================");

        try {
            LocalDate targetDate = LocalDate.now().minusDays(1);
            log.info("Daily Price Job 시작: date={}", targetDate);
            batchJobService.runManually(targetDate);
            log.info("일간 배치 스케줄러 완료");
        } catch (Exception e) {
            log.error("일간 배치 스케줄러 실패", e);
        }
    }
}
