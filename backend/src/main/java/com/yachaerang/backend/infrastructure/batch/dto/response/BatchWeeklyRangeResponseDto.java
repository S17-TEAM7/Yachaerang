package com.yachaerang.backend.infrastructure.batch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BatchWeeklyRangeResponseDto {
    private Boolean success;
    private List<Long> jobIdList;
    private List<String> statusList;
}
