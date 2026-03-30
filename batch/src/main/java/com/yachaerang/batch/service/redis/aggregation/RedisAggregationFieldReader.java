package com.yachaerang.batch.service.redis.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis Hash에서 단일 field 또는 entries 맵으로부터 long 값을 안전하게 읽는 헬퍼.
 * ClassCastException / NumberFormatException 을 방어하고 warn 로그를 남긴다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class RedisAggregationFieldReader {

    private final StringRedisTemplate redisTemplate;

    /**
     * Hash field 단건 조회.
     * NumberFormatException 발생 시 null 반환 + warn 로그.
     */
    Long getHashLong(String key, String field) {
        Object raw = redisTemplate.opsForHash().get(key, field);
        if (raw == null) return null;
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            log.warn("Redis 파싱 실패: key={}, field={}, value={}", key, field, raw);
            return null;
        }
    }

    /**
     * Hash entries 맵에서 long 값을 안전하게 읽는다.
     * NumberFormatException 발생 시 0 반환 + warn 로그.
     */
    long parseLong(Map<Object, Object> fields, String field, String hashKey) {
        Object val = fields.get(field);
        if (val == null) return 0L;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            log.warn("Redis 파싱 실패: key={}, field={}, value={}", hashKey, field, val);
            return 0L;
        }
    }

    /** sum/count 로 소수점 둘째 자리까지 반올림한 평균을 계산한다. */
    double calcAvg(Map<Object, Object> fields, String hashKey) {
        long sum   = parseLong(fields, "sum",   hashKey);
        long count = parseLong(fields, "count", hashKey);
        return count == 0 ? 0.0 : Math.round((double) sum / count * 100.0) / 100.0;
    }
}
