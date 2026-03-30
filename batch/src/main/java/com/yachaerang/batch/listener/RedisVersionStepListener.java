package com.yachaerang.batch.listener;

import com.yachaerang.batch.service.redis.RedisAggregationWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.BatchStatus;
import org.springframework.stereotype.Component;

/**
 * Redis 집계 Step의 버전 lifecycle을 관리하는 Listener
 * prefix로 해당 버저닝을 기입
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisVersionStepListener implements StepExecutionListener {

    private final RedisAggregationWriter redisAggregationWriter;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        long version = redisAggregationWriter.startNewVersion();
        log.info("Redis 집계 Step 시작: step={}, pendingVersion=v{}",
                stepExecution.getStepName(), version);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            redisAggregationWriter.commitVersion();
            log.info("Redis 집계 Step 완료: step={}, 버전 커밋",
                    stepExecution.getStepName());
        } else {
            log.warn("Redis 집계 Step 실패: step={}, status={}, 버전 미커밋 (이전 live 버전 유지)",
                    stepExecution.getStepName(), stepExecution.getStatus());
        }
        return stepExecution.getExitStatus();
    }
}
