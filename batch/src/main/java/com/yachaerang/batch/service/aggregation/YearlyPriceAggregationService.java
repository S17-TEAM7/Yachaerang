package com.yachaerang.batch.service.aggregation;

import com.yachaerang.batch.domain.entity.YearlyPrice;
import com.yachaerang.batch.repository.YearlyPriceRepository;
import com.yachaerang.batch.service.redis.aggregation.RedisAggregationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class YearlyPriceAggregationService {

    private final YearlyPriceRepository yearlyPriceRepository;
    private final RedisAggregationQueryService redisAggregationQueryService;

    /**
     * 연간 집계 데이터 조회
     */
    public List<YearlyPrice> getYearlyAggregatedPrices(int year) {
        log.info("연간 집계 데이터 조회 - 연도: {}", year);
        List<YearlyPrice> result = redisAggregationQueryService.getYearlyAggregatedPrices(year);
        log.info("조회된 집계 데이터: {} 건", result.size());
        return result;
    }

    /**
     * 연초 가격 조회
     */
    public Long getStartPrice(String productCode, int year) {
        return redisAggregationQueryService.getYearlyStartPrice(productCode, year);
    }

    /**
     * 연말 가격 조회
     */
    public Long getEndPrice(String productCode, int year) {
        return redisAggregationQueryService.getYearlyEndPrice(productCode, year);
    }

    /**
     * 연간 가격 데이터 저장
     */
    @Transactional
    public void saveYearlyPrices(List<YearlyPrice> yearlyPrices) {
        for (YearlyPrice yearlyPrice : yearlyPrices) {
            yearlyPriceRepository.upsertYearlyPrice(yearlyPrice);
        }
        log.info("{} 건 저장 완료", yearlyPrices.size());
    }
}
