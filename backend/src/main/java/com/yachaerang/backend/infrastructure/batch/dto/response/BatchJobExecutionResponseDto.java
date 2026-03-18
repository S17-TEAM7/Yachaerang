package com.yachaerang.backend.infrastructure.batch.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchJobExecutionResponseDto {
    private Boolean success;
    private Long jobId;
    private String status;
    private String startDate;
    private String endDate;
    private String year;
    private String week;
    private String month;
}
