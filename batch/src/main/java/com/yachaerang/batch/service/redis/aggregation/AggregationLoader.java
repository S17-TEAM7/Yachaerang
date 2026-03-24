package com.yachaerang.batch.service.redis.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Index Set 조회 후 Hash 조회 후 Entity들로 Loader
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AggregationLoader {

    private final StringRedisTemplate redisTemplate;
    private final RedisAggregationFieldReader fieldReader;

    /**
     * @param indexKey  idx:{type}:... 형식의 Index Set Key
     * @param logLabel  로그 식별용 레이블
     * @param toHashKey productCode → hash 키 변환 함수
     * @param mapper    (productCode, hashKey, fields) → 엔티티
     */
    <T> List<T> load(String indexKey, String logLabel,
            Function<String, String> toHashKey, EntityMapper<T> mapper) {

        Set<String> productCodes = redisTemplate.opsForSet().members(indexKey);
        if (productCodes == null || productCodes.isEmpty()) {
            log.info("Redis {} 집계 조회: total=0", logLabel);
            return new ArrayList<>();
        }

        List<T> results = new ArrayList<>();
        int stale = 0, empty = 0;
        for (String productCode : productCodes) {
            String hashKey = toHashKey.apply(productCode);
            Map<Object, Object> fields = redisTemplate.opsForHash().entries(hashKey);
            if (fields.isEmpty()) {
                log.warn("Redis stale 인덱스: hash 없음 key={}", hashKey);
                stale++;
                continue;
            }
            long count = fieldReader.parseLong(fields, "count", hashKey);
            if (count == 0) { empty++; continue; }

            results.add(mapper.map(productCode, hashKey, fields));
        }
        log.info("Redis {} 집계 조회: total={}, result={}, stale={}, empty={}",
                logLabel, productCodes.size(), results.size(), stale, empty);
        return results;
    }
}
