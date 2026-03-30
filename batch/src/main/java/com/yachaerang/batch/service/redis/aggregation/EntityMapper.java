package com.yachaerang.batch.service.redis.aggregation;

import java.util.Map;

/**
 * Redis Hash fields에서 Entity로 반환하는 Mapper
 */
@FunctionalInterface
interface EntityMapper<T> {
    T map(String productCode, String hashKey, Map<Object, Object> fields);
}
