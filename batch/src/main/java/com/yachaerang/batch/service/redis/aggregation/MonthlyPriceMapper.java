package com.yachaerang.batch.service.redis.aggregation;

import com.yachaerang.batch.domain.entity.MonthlyPrice;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Hash fields에서 EntityMapper를 통해 MonthlyPrice를 반환
 */
@RequiredArgsConstructor
class MonthlyPriceMapper implements EntityMapper<MonthlyPrice> {

    private final int year;
    private final int month;
    private final RedisAggregationFieldReader fieldReader;

    @Override
    public MonthlyPrice map(String productCode, String hashKey, Map<Object, Object> fields) {
        return MonthlyPrice.builder()
                .productCode(productCode)
                .priceYear(year)
                .priceMonth(month)
                .avgPrice(fieldReader.calcAvg(fields, hashKey))
                .minPrice(fieldReader.parseLong(fields, "min",   hashKey))
                .maxPrice(fieldReader.parseLong(fields, "max",   hashKey))
                .priceCount((int) fieldReader.parseLong(fields, "count", hashKey))
                .build();
    }
}
