package com.yachaerang.batch.domain.processor;

import com.yachaerang.batch.domain.entity.WeeklyPrice;
import com.yachaerang.batch.service.redis.aggregation.RedisAggregationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@RequiredArgsConstructor
public class WeeklyPriceProcessor implements ItemProcessor<WeeklyPrice, WeeklyPrice> {

    private final RedisAggregationQueryService redisAggregationQueryService;

    @Override
    public WeeklyPrice process(WeeklyPrice item) throws Exception {

        // null 확인
        if (item.getPriceCount() == 0) {
            log.debug("priceCount가 0이므로 스킵: {}", item.getProductCode());
            return null;
        }

        String productCode = item.getProductCode();
        int year   = item.getPriceYear();
        int weekNum = item.getWeekNumber();

        // 해당 주차의 시작 가격 조회
        Long startPrice = redisAggregationQueryService.getWeeklyStartPrice(productCode, year, weekNum);
        // 해당 주차의 종료 가격 조회
        Long endPrice = redisAggregationQueryService.getWeeklyEndPrice(productCode, year, weekNum);

        // 계산
        Long priceChange = 0L;
        BigDecimal priceChangeRate = new BigDecimal(0.00);

        if (startPrice != null && endPrice != null && startPrice > 0) {
            priceChange = endPrice - startPrice;
            priceChangeRate = BigDecimal.valueOf(priceChange)
                    .divide(BigDecimal.valueOf(startPrice), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            log.debug("주간 변화량: {} , 변화율: {}", priceChange, priceChangeRate);
        } else {
            log.debug("주간 변화 계산 불가 (데이터 부족): productCode={}, startPrice={}, endPrice={}",
                    productCode, startPrice, endPrice);
        }
        return WeeklyPrice.builder()
                .productCode(productCode)
                .priceYear(year)
                .weekNumber(weekNum)
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .avgPrice(item.getAvgPrice())
                .minPrice(item.getMinPrice())
                .maxPrice(item.getMaxPrice())
                .priceCount(item.getPriceCount())
                .priceChange(priceChange)
                .priceChangeRate(priceChangeRate)
                .build();
    }
}
