package com.yachaerang.batch.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class JobStatusResponseDto {
    private Long jobId;
    private String jobName;
    private String status;
    private String exitStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String message;
    private String failureReason;
}
