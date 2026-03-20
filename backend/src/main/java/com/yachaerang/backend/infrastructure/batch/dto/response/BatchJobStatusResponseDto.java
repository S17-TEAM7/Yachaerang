package com.yachaerang.backend.infrastructure.batch.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchJobStatusResponseDto {
    private Long jobId;
    private String jobName;
    private String status;
    private String exitStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String message;
    private String failureReason;
}
