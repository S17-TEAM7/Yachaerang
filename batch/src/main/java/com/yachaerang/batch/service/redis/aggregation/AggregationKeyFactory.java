package com.yachaerang.batch.service.redis.aggregation;

import org.springframework.stereotype.Component;

import static com.yachaerang.batch.service.redis.RedisAggregationKeys.*;

/**
 * 집계/인덱스 Redis Key 문자열 생성 Factory
 */
@Component
class AggregationKeyFactory {

    String weeklyHashKey(String productCode, int year, int weekNum) {
        return String.format(AGG_WEEKLY_KEY, productCode, year, weekNum);
    }

    String monthlyHashKey(String productCode, int year, int month) {
        return String.format(AGG_MONTHLY_KEY, productCode, year, month);
    }

    String yearlyHashKey(String productCode, int year) {
        return String.format(AGG_YEARLY_KEY, productCode, year);
    }

    String weeklyIndexKey(int year, int weekNum) {
        return String.format(IDX_WEEKLY_KEY, year, weekNum);
    }

    String monthlyIndexKey(int year, int month) {
        return String.format(IDX_MONTHLY_KEY, year, month);
    }

    String yearlyIndexKey(int year) {
        return String.format(IDX_YEARLY_KEY, year);
    }
}
