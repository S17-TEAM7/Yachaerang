package com.yachaerang.batch.service.redis.aggregation;

import org.springframework.stereotype.Component;

import static com.yachaerang.batch.service.redis.RedisAggregationKeys.*;

/**
 * 집계/인덱스 Redis Key 문자열 생성 Factory
 */
@Component
class AggregationKeyFactory {

    String weeklyHashKey(long version, String productCode, int year, int weekNum) {
        return String.format(AGG_WEEKLY_KEY, version, productCode, year, weekNum);
    }

    String monthlyHashKey(long version, String productCode, int year, int month) {
        return String.format(AGG_MONTHLY_KEY, version, productCode, year, month);
    }

    String yearlyHashKey(long version, String productCode, int year) {
        return String.format(AGG_YEARLY_KEY, version, productCode, year);
    }

    String weeklyIndexKey(long version, int year, int weekNum) {
        return String.format(IDX_WEEKLY_KEY, version, year, weekNum);
    }

    String monthlyIndexKey(long version, int year, int month) {
        return String.format(IDX_MONTHLY_KEY, version, year, month);
    }

    String yearlyIndexKey(long version, int year) {
        return String.format(IDX_YEARLY_KEY, version, year);
    }
}
