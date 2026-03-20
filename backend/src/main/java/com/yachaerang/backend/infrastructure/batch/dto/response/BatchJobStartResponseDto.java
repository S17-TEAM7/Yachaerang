package com.yachaerang.backend.infrastructure.batch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobStartResponseDto {
    private Long jobId;
    private String status;
    private String message;
}
