package com.yachaerang.batch.service.redis.aggregation;

import com.yachaerang.batch.domain.entity.YearlyPrice;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Hash fields에서 EntityMapper를 통해 YearlyPrice를 반환
 */
@RequiredArgsConstructor
class YearlyPriceMapper implements EntityMapper<YearlyPrice> {

    private final int year;
    private final RedisAggregationFieldReader fieldReader;

    @Override
    public YearlyPrice map(String productCode, String hashKey, Map<Object, Object> fields) {
        return YearlyPrice.builder()
                .productCode(productCode)
                .priceYear(year)
                .avgPrice(fieldReader.calcAvg(fields, hashKey))
                .minPrice(fieldReader.parseLong(fields, "min",   hashKey))
                .maxPrice(fieldReader.parseLong(fields, "max",   hashKey))
                .priceCount((int) fieldReader.parseLong(fields, "count", hashKey))
                .build();
    }
}
