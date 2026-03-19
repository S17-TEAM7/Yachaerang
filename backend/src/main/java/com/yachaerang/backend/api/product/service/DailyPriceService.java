package com.yachaerang.backend.api.product.service;

import com.yachaerang.backend.api.product.dto.response.DailyPriceResponseDto;
import com.yachaerang.backend.api.product.repository.DailyPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPriceService {

    private final DailyPriceMapper dailyPriceMapper;

    /*
    해당 기간 동안의 가격 전부 가져오기
     */
    @Cacheable(
            value = "daily:price",
            key = "#productCode + ':' + #startDate + ':' + #endDate",
            condition = "#endDate.isBefore(T(java.time.LocalDate).now())"
    )
    public List<DailyPriceResponseDto.PriceRecordDto> getDailyPrice(
            String productCode, LocalDate startDate, LocalDate endDate) {
        return dailyPriceMapper.getPriceDuration(productCode, startDate, endDate);
    }

    /*
    어제자를 기준으로 가격이 높은 것부터 보여주기
    날짜를 키로 사용하여 날짜가 바뀌면 자동으로 새 캐시 생성
     */
    @Cacheable(value = "daily:rank:high", key = "#date")
    public List<DailyPriceResponseDto.RankDto> getHighPriceRank(LocalDate date) {
        return dailyPriceMapper.getPricesDescending(date);
    }

    /*
    어제자를 기준으로 가격이 낮은 것부터 보여주기
     */
    @Cacheable(value = "daily:rank:low", key = "#date")
    public List<DailyPriceResponseDto.RankDto> getLowPriceRank(LocalDate date) {
        return dailyPriceMapper.getPricesAscending(date);
    }
}
