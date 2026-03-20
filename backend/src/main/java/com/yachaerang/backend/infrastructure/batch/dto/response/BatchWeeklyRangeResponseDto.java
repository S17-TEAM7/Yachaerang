package com.yachaerang.backend.infrastructure.batch.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchWeeklyRangeResponseDto {
    private Boolean success;
    private List<Long> jobIdList;
    private List<String> statusList;
}
