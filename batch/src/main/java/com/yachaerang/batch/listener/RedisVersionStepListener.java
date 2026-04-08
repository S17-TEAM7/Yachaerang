package com.yachaerang.batch.listener;

import com.yachaerang.batch.service.redis.RedisAggregationWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.BatchStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis 집계 Step의 버전 lifecycle을 관리하는 Listener
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisVersionStepListener implements StepExecutionListener {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RedisAggregationWriter redisAggregationWriter;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LocalDate referenceDate = resolveReferenceDate(stepExecution);
        long version = redisAggregationWriter.startNewVersion(referenceDate);
        log.info("Redis 집계 Step 시작: step={}, pendingVersion=v{}",
                stepExecution.getStepName(), version);
    }

    /**
     * endDate → targetDate → 오늘 순으로 기준 날짜를 결정한다.
     */
    private LocalDate resolveReferenceDate(StepExecution stepExecution) {
        String dateStr = stepExecution.getJobParameters().getString("endDate");
        if (dateStr == null || dateStr.isBlank()) {
            dateStr = stepExecution.getJobParameters().getString("targetDate");
        }
        return (dateStr != null && !dateStr.isBlank())
                ? LocalDate.parse(dateStr, FORMATTER)
                : LocalDate.now();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            redisAggregationWriter.commitVersion();
            log.info("Redis 집계 Step 완료: step={}, 버전 커밋",
                    stepExecution.getStepName());
        } else {
            redisAggregationWriter.resetPendingVersion();
            log.warn("Redis 집계 Step 실패: step={}, status={}, 이전 버전을 유지",
                    stepExecution.getStepName(), stepExecution.getStatus());
        }
        return stepExecution.getExitStatus();
    }
}
