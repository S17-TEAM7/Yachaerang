package com.yachaerang.batch.service.redis;

import com.yachaerang.batch.domain.entity.DailyPrice;
import com.yachaerang.batch.util.WeekUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.yachaerang.batch.service.redis.RedisAggregationKeys.*;

/**
 * daily_price 저장 시점에 Redis Hash로 주간/월간/연간 집계를 실시간 누적하는 Writer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisAggregationWriter {

    private final StringRedisTemplate redisTemplate;

    private static final DateTimeFormatter DATE_SCORE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final long TTL_WEEKLY_SECONDS  = 14L  * 24 * 3600;  //  2주
    private static final long TTL_MONTHLY_SECONDS = 35L  * 24 * 3600;  // 35일
    private static final long TTL_YEARLY_SECONDS  = 400L * 24 * 3600;  // 400일

    /**
     * 현재 쓰기 중인 버전 번호로 버저닝
     */
    private volatile long pendingVersion = 0L;

    /**
     * 새 버전을 할당하기 위한 시작 때 호출하는 메서드 (Redis INCR)
     */
    public long startNewVersion() {
        Long v = redisTemplate.opsForValue().increment(VERSION_SEQ_KEY);
        pendingVersion = (v != null) ? v : 1L;
        log.info("Redis 집계 새 버전 할당: v{}", pendingVersion);
        return pendingVersion;
    }

    /**
     * Redis의 Step 완료 후 버전 호출
     */
    public void commitVersion() {
        redisTemplate.opsForValue().set(ACTIVE_VERSION_KEY, String.valueOf(pendingVersion));
        log.info("Redis 집계 버전 커밋: v{}", pendingVersion);
    }

    /**
     * 활성화 버전 정보 얻기
     */
    public long getActiveVersion() {
        String v = redisTemplate.opsForValue().get(ACTIVE_VERSION_KEY);
        if (v == null) return 0L;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            log.warn("agg:active_version 파싱 실패: value={}", v);
            return 0L;
        }
    }

    /**
     * KEYS[1] : weekly  hash key  (v{N}:agg:weekly:...)
     * KEYS[2] : monthly hash key  (v{N}:agg:monthly:...)
     * KEYS[3] : yearly  hash key  (v{N}:agg:yearly:...)
     * KEYS[4] : weekly  index set key  (v{N}:idx:weekly:...)
     * KEYS[5] : monthly index set key  (v{N}:idx:monthly:...)
     * KEYS[6] : yearly  index set key  (v{N}:idx:yearly:...)
     * ARGV[1] : price
     * ARGV[2] : date score (yyyyMMdd)
     * ARGV[3] : weekly  TTL (seconds)
     * ARGV[4] : monthly TTL (seconds)
     * ARGV[5] : yearly  TTL (seconds)
     * ARGV[6] : productCode
     *
     * <p>버전 prefix로 키가 격리되므로 별도의 :processed set 중복 체크가 불필요하다.
     */
    private static final String AGG_LUA = """
        local function agg(hash_key, idx_key, ttl)
            local price      = tonumber(ARGV[1])
            local date_score = tonumber(ARGV[2])

            redis.call('HINCRBY', hash_key, 'sum', ARGV[1])
            local new_count = redis.call('HINCRBY', hash_key, 'count', 1)

            if new_count == 1 then
                redis.call('EXPIRE', hash_key, ttl)
                redis.call('SADD', idx_key, ARGV[6])
                if redis.call('TTL', idx_key) == -1 then
                    redis.call('EXPIRE', idx_key, ttl)
                end
            end

            local cur_min = redis.call('HGET', hash_key, 'min')
            local cur_max = redis.call('HGET', hash_key, 'max')

            if not cur_min or price < tonumber(cur_min) then
                redis.call('HSET', hash_key, 'min', ARGV[1])
            end

            if not cur_max or price > tonumber(cur_max) then
                redis.call('HSET', hash_key, 'max', ARGV[1])
            end

            local cur_fd = redis.call('HGET', hash_key, 'first_date')
            if not cur_fd or date_score < tonumber(cur_fd) then
                redis.call('HSET', hash_key, 'first_date', ARGV[2])
                redis.call('HSET', hash_key, 'first_price', ARGV[1])
            end

            local cur_ld = redis.call('HGET', hash_key, 'last_date')
            if not cur_ld or date_score > tonumber(cur_ld) then
                redis.call('HSET', hash_key, 'last_date', ARGV[2])
                redis.call('HSET', hash_key, 'last_price', ARGV[1])
            end
        end

        agg(KEYS[1], KEYS[4], ARGV[3])
        agg(KEYS[2], KEYS[5], ARGV[4])
        agg(KEYS[3], KEYS[6], ARGV[5])

        return 1
        """;

    private static final RedisScript<Long> AGG_SCRIPT = RedisScript.of(AGG_LUA, Long.class);

    public void updateAggregations(DailyPrice dailyPrice) {
        String productCode = dailyPrice.getProductCode();
        LocalDate priceDate = dailyPrice.getPriceDate();
        String priceStr  = String.valueOf(dailyPrice.getPrice());
        String dateScore = priceDate.format(DATE_SCORE_FMT);

        int[] yw     = WeekUtils.getYearAndWeek(priceDate);
        int weekYear = yw[0];
        int weekNum  = yw[1];
        int year     = priceDate.getYear();
        int month    = priceDate.getMonthValue();

        long v = pendingVersion;

        try {
            redisTemplate.execute(AGG_SCRIPT,
                    List.of(
                            String.format(AGG_WEEKLY_KEY,  v, productCode, weekYear, weekNum),
                            String.format(AGG_MONTHLY_KEY, v, productCode, year, month),
                            String.format(AGG_YEARLY_KEY,  v, productCode, year),
                            String.format(IDX_WEEKLY_KEY,  v, weekYear, weekNum),
                            String.format(IDX_MONTHLY_KEY, v, year, month),
                            String.format(IDX_YEARLY_KEY,  v, year)
                    ),
                    priceStr, dateScore,
                    String.valueOf(TTL_WEEKLY_SECONDS),
                    String.valueOf(TTL_MONTHLY_SECONDS),
                    String.valueOf(TTL_YEARLY_SECONDS),
                    productCode);
            log.debug("Redis 집계 업데이트: v={}, productCode={}, date={}", v, productCode, priceDate);
        } catch (Exception e) {
            log.error("Redis 집계 업데이트 실패: v={}, productCode={}, date={}", v, productCode, priceDate, e);
            throw e;
        }
    }
}
