package com.yachaerang.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MdcStepListener implements org.springframework.batch.core.StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        MDC.put("jobExecutionId", String.valueOf(stepExecution.getJobExecutionId()));
        MDC.put("stepName", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        MDC.remove("jobExecutionId");
        MDC.remove("stepName");
        return stepExecution.getExitStatus();
    }
}
