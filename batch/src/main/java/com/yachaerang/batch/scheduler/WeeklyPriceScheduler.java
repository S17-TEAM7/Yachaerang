package com.yachaerang.batch.scheduler;

import com.yachaerang.batch.service.BatchJobService;
import com.yachaerang.batch.util.WeekUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyPriceScheduler {

    private final BatchJobService batchJobService;

    /**
     * 지난 주 데이터 집계
     */
    @Scheduled(cron = "0 0 1 ? * MON")  // 매주 월요일 새벽 1시
    public void runLastWeekAggregation() {
        log.info("========================================");
        log.info("주간 배치 스케줄러 시작: {}", LocalDate.now());
        log.info("========================================");

        try {
            int[] todayYearWeek = WeekUtils.getYearAndWeek(LocalDate.now());
            int targetYear;
            int targetWeek;

            if (todayYearWeek[1] == 1) {
                targetYear = todayYearWeek[0] - 1;
                targetWeek = WeekUtils.getLastIsoWeekOfYear(targetYear);
            } else {
                targetYear = todayYearWeek[0];
                targetWeek = todayYearWeek[1] - 1;
            }

            log.info("지난 주({}년도 {}주차) 가격 집계 시작", targetYear, targetWeek);
            batchJobService.runWeeklyAggregation(targetYear, targetWeek);
            log.info("주간 배치 스케줄러 완료");
        } catch (Exception e) {
            log.error("주간 배치 스케줄러 실패", e);
        }
    }
}
