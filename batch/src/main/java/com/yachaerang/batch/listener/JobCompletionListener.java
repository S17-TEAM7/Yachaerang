package com.yachaerang.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
public class JobCompletionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("========================================");
        log.info("Job 시작: {}", jobExecution.getJobInstance().getJobName());
        log.info("jobExecutionId={}", jobExecution.getId());
        log.info("Job Parameters: {}", jobExecution.getJobParameters());
        log.info("시작 시간: {}", jobExecution.getStartTime());
        log.info("========================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();
        Duration duration = Duration.between(startTime, endTime);

        log.info("========================================");
        log.info("Job 완료: {}", jobExecution.getJobInstance().getJobName());
        log.info("jobExecutionId={}, status={}", jobExecution.getId(), jobExecution.getStatus());
        log.info("종료 시간: {}", endTime);
        log.info("소요 시간: {}초", duration.getSeconds());

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("===== JOB FAILED =====");
            log.error("jobName={}, jobExecutionId={}, status={}",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getId(),
                    jobExecution.getStatus());

            // 모든 failure exception 순회하며 root cause 탐색
            for (Throwable ex : jobExecution.getAllFailureExceptions()) {
                log.error("Failure exception: class={}", ex.getClass().getName(), ex);

                Throwable root = ex;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                log.error("Root cause: class={}, message={}", root.getClass().getName(), root.getMessage());
            }

            // 실패한 StepExecution 상세 출력
            jobExecution.getStepExecutions().stream()
                    .filter(step -> step.getStatus() == BatchStatus.FAILED)
                    .forEach(step -> log.error(
                            "Failed step: name={}, readCount={}, writeCount={}, skipCount={}, exitDescription={}",
                            step.getStepName(),
                            step.getReadCount(),
                            step.getWriteCount(),
                            step.getSkipCount(),
                            step.getExitStatus().getExitDescription()
                    ));
        }
        log.info("========================================");
    }
}
