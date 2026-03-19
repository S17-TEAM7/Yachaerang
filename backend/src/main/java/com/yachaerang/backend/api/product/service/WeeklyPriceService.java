package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.WeeklyPriceResponseDto;
import com.yachaerang.backend.api.product.repository.WeeklyPriceMapper;
import com.yachaerang.backend.global.exception.CustomException;
import com.yachaerang.backend.global.exception.GeneralException;
import com.yachaerang.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WeeklyPriceService {

    private final WeeklyPriceMapper weeklyPriceMapper;

    @Cacheable(
            value = "weekly:price",
            key = "#productCode + ':' + #startDate + ':' + #endDate",
            condition = "#endDate.isBefore(T(java.time.LocalDate).now())"
    )
    public List<WeeklyPriceResponseDto.PriceRecordDto> getPriceDuration(
            String productCode, LocalDate startDate, LocalDate endDate
    ) {
        // startDate 검증
        boolean isFirstOfWeek = (startDate.getDayOfWeek() == DayOfWeek.MONDAY);
        if (!isFirstOfWeek) {
            throw CustomException.of(ErrorCode.INVALID_ISO_WEEK_DATE);
        }
        // endDate 검증
        boolean isEndOfWeek =(endDate.getDayOfWeek() == DayOfWeek.SUNDAY);
        if (!isEndOfWeek) {
            throw CustomException.of(ErrorCode.INVALID_ISO_WEEK_DATE);
        }

        // startDate > endDate
        if (endDate.isBefore(startDate)) {
            throw GeneralException.of(ErrorCode.WRONG_REQUEST_DATE);
        }
        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today) || endDate.isAfter(today)) {
            throw GeneralException.of(ErrorCode.WRONG_REQUEST_DATE);
        }

        return weeklyPriceMapper.getPriceDuration(productCode, startDate, endDate);
    }
}
