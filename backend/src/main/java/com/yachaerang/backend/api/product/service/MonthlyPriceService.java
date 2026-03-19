package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.MonthlyPriceResponseDto;
import com.yachaerang.backend.api.product.repository.MonthlyPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyPriceService {

    private final MonthlyPriceMapper monthlyPriceMapper;

    /*
    종료 연월이 현재 월 이전인 경우만 캐시
     */
    @Cacheable(
            value = "monthly:price",
            key = "#productCode + ':' + #startYear + '-' + #startMonth + ':' + #endYear + '-' + #endMonth",
            condition = "#endYear < T(java.time.LocalDate).now().getYear() or " +
                        "(#endYear == T(java.time.LocalDate).now().getYear() and " +
                        " #endMonth < T(java.time.LocalDate).now().getMonthValue())"
    )
    public List<MonthlyPriceResponseDto.PriceDto> getMonthlyPrices(
            String productCode, int startYear, int startMonth,
            int endYear, int endMonth
    ) {
        return monthlyPriceMapper.getPriceDuration(productCode, startYear, startMonth, endYear, endMonth);
    }
}
