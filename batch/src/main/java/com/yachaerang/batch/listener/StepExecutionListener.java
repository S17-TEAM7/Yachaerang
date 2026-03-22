package com.yachaerang.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StepExecutionListener implements org.springframework.batch.core.StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Step 시작: name={}, jobExecutionId={}",
                stepExecution.getStepName(),
                stepExecution.getJobExecutionId());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.FAILED) {
            log.error("Step 실패: name={}, readCount={}, writeCount={}, skipCount={}, rollbackCount={}",
                    stepExecution.getStepName(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getSkipCount(),
                    stepExecution.getRollbackCount());

            stepExecution.getFailureExceptions().forEach(ex ->
                    log.error("Step failure exception: class={}, message={}",
                            ex.getClass().getName(), ex.getMessage(), ex)
            );

            log.error("Step ExecutionContext: {}", stepExecution.getExecutionContext());
        } else {
            log.info("Step 완료: name={}, readCount={}, writeCount={}, skipCount={}",
                    stepExecution.getStepName(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getSkipCount());
        }
        return stepExecution.getExitStatus();
    }
}
