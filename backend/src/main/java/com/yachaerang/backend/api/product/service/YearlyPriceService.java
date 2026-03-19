package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.YearlyPriceResponseDto;
import com.yachaerang.backend.api.product.repository.YearlyPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YearlyPriceService {

    private final YearlyPriceMapper yearlyPriceMapper;

    /*
    종료 연도가 올해 이전인 경우만 캐시
     */
    @Cacheable(
            value = "yearly:price",
            key = "#productCode + ':' + #startYear + ':' + #endYear",
            condition = "#endYear < T(java.time.LocalDate).now().getYear()"
    )
    public List<YearlyPriceResponseDto.PriceDto> getPrices(
            String productCode, int startYear, int endYear
    ) {
        return yearlyPriceMapper.getPriceDuration(productCode, startYear, endYear);
    }

    /*
    대상 연도가 올해 이전인 경우만 캐시
     */
    @Cacheable(
            value = "yearly:price",
            key = "#productCode + ':' + #targetYear",
            condition = "#targetYear < T(java.time.LocalDate).now().getYear()"
    )
    public YearlyPriceResponseDto.PriceDto getPrice(
            String productCode, int targetYear
    ) {
        return yearlyPriceMapper.getPriceSpecification(productCode, targetYear);
    }
}
