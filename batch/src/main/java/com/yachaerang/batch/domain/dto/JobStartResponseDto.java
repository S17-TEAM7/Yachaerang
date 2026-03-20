package com.yachaerang.batch.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class JobStartResponseDto {
    private Long jobId;
    private String status;
    private String message;
}
