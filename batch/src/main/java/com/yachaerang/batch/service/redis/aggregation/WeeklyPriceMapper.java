package com.yachaerang.batch.service.redis.aggregation;

import com.yachaerang.batch.domain.entity.WeeklyPrice;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Hash fields에서 EntityMapper를 통해 WeeklyPrice 반환
 */
@RequiredArgsConstructor
class WeeklyPriceMapper implements EntityMapper<WeeklyPrice> {

    private final int year;
    private final int weekNum;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final RedisAggregationFieldReader fieldReader;

    @Override
    public WeeklyPrice map(String productCode, String hashKey, Map<Object, Object> fields) {
        return WeeklyPrice.builder()
                .productCode(productCode)
                .priceYear(year)
                .weekNumber(weekNum)
                .startDate(startDate)
                .endDate(endDate)
                .avgPrice(fieldReader.calcAvg(fields, hashKey))
                .minPrice(fieldReader.parseLong(fields, "min",   hashKey))
                .maxPrice(fieldReader.parseLong(fields, "max",   hashKey))
                .priceCount((int) fieldReader.parseLong(fields, "count", hashKey))
                .build();
    }
}
