package com.yachaerang.batch.service;

import com.yachaerang.batch.domain.dto.JobStatusResponseDto;
import com.yachaerang.batch.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobStatusService {

    private final JobExplorer jobExplorer;

    public JobStatusResponseDto getJobStatus(Long jobId) {
        JobExecution execution = jobExplorer.getJobExecution(jobId);
        if (execution == null) {
            throw new GeneralException("존재하지 않는 jobId입니다: " + jobId);
        }

        log.info("Job 상태 조회: jobId={}, status={}", jobId, execution.getStatus());

        String failureReason = execution.getAllFailureExceptions().stream()
                .findFirst()
                .map(Throwable::getMessage)
                .orElse(null);

        return JobStatusResponseDto.builder()
                .jobId(jobId)
                .jobName(execution.getJobInstance().getJobName())
                .status(execution.getStatus().toString())
                .exitStatus(execution.getExitStatus().getExitCode())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .message(resolveMessage(execution.getStatus()))
                .failureReason(failureReason)
                .build();
    }

    private String resolveMessage(BatchStatus status) {
        return switch (status) {
            case STARTING, STARTED -> "Job is running";
            case COMPLETED -> "Job completed successfully";
            case FAILED -> "Job failed";
            case STOPPED -> "Job stopped";
            default -> status.toString();
        };
    }
}
