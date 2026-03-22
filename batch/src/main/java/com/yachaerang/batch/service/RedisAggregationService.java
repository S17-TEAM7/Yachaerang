package com.yachaerang.batch.service;

import com.yachaerang.batch.domain.entity.DailyPrice;
import com.yachaerang.batch.domain.entity.MonthlyPrice;
import com.yachaerang.batch.domain.entity.WeeklyPrice;
import com.yachaerang.batch.domain.entity.YearlyPrice;
import com.yachaerang.batch.util.WeekUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * daily_price 저장 시점에 Redis Hash로 주간/월간/연간 집계를 실시간 누적하고,
 * 집계 Job 실행 시 DB GROUP BY 없이 Redis에서 읽어오는 서비스.
 *
 * Hash key 형식:
 *   agg:weekly:{productCode}:{year}:{weekNum}
 *   agg:monthly:{productCode}:{year}:{month}
 *   agg:yearly:{productCode}:{year}
 *
 * Hash fields: sum, count, min, max, first_date, first_price, last_date, last_price
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisAggregationService {

    private final StringRedisTemplate redisTemplate;

    private static final DateTimeFormatter DATE_SCORE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String AGG_WEEKLY_KEY  = "agg:weekly:%s:%d:%d";
    private static final String AGG_MONTHLY_KEY = "agg:monthly:%s:%d:%d";
    private static final String AGG_YEARLY_KEY  = "agg:yearly:%s:%d";

    /**
     * 원자적으로 sum/count/min/max/first_price/last_price 를 갱신하는 Lua 스크립트.
     * KEYS[1] : Hash key
     * ARGV[1] : price (Long as String)
     * ARGV[2] : date score (yyyyMMdd as String)
     */
    private static final String AGG_LUA =
            "local price      = tonumber(ARGV[1]) " +
            "local date_score = tonumber(ARGV[2]) " +
            "redis.call('HINCRBY', KEYS[1], 'sum',   ARGV[1]) " +
            "redis.call('HINCRBY', KEYS[1], 'count', 1) " +
            "local cur_min = redis.call('HGET', KEYS[1], 'min') " +
            "local cur_max = redis.call('HGET', KEYS[1], 'max') " +
            "if not cur_min or price < tonumber(cur_min) then " +
            "    redis.call('HSET', KEYS[1], 'min', ARGV[1]) " +
            "end " +
            "if not cur_max or price > tonumber(cur_max) then " +
            "    redis.call('HSET', KEYS[1], 'max', ARGV[1]) " +
            "end " +
            "local cur_fd = redis.call('HGET', KEYS[1], 'first_date') " +
            "if not cur_fd or date_score < tonumber(cur_fd) then " +
            "    redis.call('HSET', KEYS[1], 'first_date',  ARGV[2]) " +
            "    redis.call('HSET', KEYS[1], 'first_price', ARGV[1]) " +
            "end " +
            "local cur_ld = redis.call('HGET', KEYS[1], 'last_date') " +
            "if not cur_ld or date_score > tonumber(cur_ld) then " +
            "    redis.call('HSET', KEYS[1], 'last_date',  ARGV[2]) " +
            "    redis.call('HSET', KEYS[1], 'last_price', ARGV[1]) " +
            "end " +
            "return 1";

    private static final RedisScript<Long> AGG_SCRIPT = RedisScript.of(AGG_LUA, Long.class);

    // Write: DailyPriceWriter에서 저장 후 호출
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

        redisTemplate.execute(AGG_SCRIPT,
                List.of(String.format(AGG_WEEKLY_KEY, productCode, weekYear, weekNum)),
                priceStr, dateScore);
        redisTemplate.execute(AGG_SCRIPT,
                List.of(String.format(AGG_MONTHLY_KEY, productCode, year, month)),
                priceStr, dateScore);
        redisTemplate.execute(AGG_SCRIPT,
                List.of(String.format(AGG_YEARLY_KEY, productCode, year)),
                priceStr, dateScore);

        log.debug("Redis 집계 업데이트: productCode={}, date={}", productCode, priceDate);
    }

    // Read: 집계 데이터 조회 (AggregationService에서 호출)
    public List<WeeklyPrice> getWeeklyAggregatedPrices(LocalDate startDate, LocalDate endDate) {
        int[] yw    = WeekUtils.getYearAndWeek(startDate);
        int year    = yw[0];
        int weekNum = yw[1];
        String pattern = String.format("agg:weekly:*:%d:%d", year, weekNum);

        List<WeeklyPrice> results = new ArrayList<>();
        for (String key : scanKeys(pattern)) {
            Map<Object, Object> fields = redisTemplate.opsForHash().entries(key);
            if (fields.isEmpty()) continue;

            long sum   = parseLong(fields, "sum");
            long count = parseLong(fields, "count");
            if (count == 0) continue;

            double avg = Math.round((double) sum / count * 100.0) / 100.0;
            results.add(WeeklyPrice.builder()
                    .productCode(extractProductCode(key))
                    .priceYear(year)
                    .weekNumber(weekNum)
                    .startDate(startDate)
                    .endDate(endDate)
                    .avgPrice(avg)
                    .minPrice(parseLong(fields, "min"))
                    .maxPrice(parseLong(fields, "max"))
                    .priceCount((int) count)
                    .build());
        }
        log.info("Redis 주간 집계 조회: {}건 (year={}, week={})", results.size(), year, weekNum);
        return results;
    }

    public List<MonthlyPrice> getMonthlyAggregatedPrices(int year, int month) {
        String pattern = String.format("agg:monthly:*:%d:%d", year, month);

        List<MonthlyPrice> results = new ArrayList<>();
        for (String key : scanKeys(pattern)) {
            Map<Object, Object> fields = redisTemplate.opsForHash().entries(key);
            if (fields.isEmpty()) continue;

            long sum   = parseLong(fields, "sum");
            long count = parseLong(fields, "count");
            if (count == 0) continue;

            double avg = Math.round((double) sum / count * 100.0) / 100.0;
            results.add(MonthlyPrice.builder()
                    .productCode(extractProductCode(key))
                    .priceYear(year)
                    .priceMonth(month)
                    .avgPrice(avg)
                    .minPrice(parseLong(fields, "min"))
                    .maxPrice(parseLong(fields, "max"))
                    .priceCount((int) count)
                    .build());
        }
        log.info("Redis 월간 집계 조회: {}건 (year={}, month={})", results.size(), year, month);
        return results;
    }

    public List<YearlyPrice> getYearlyAggregatedPrices(int year) {
        String pattern = String.format("agg:yearly:*:%d", year);

        List<YearlyPrice> results = new ArrayList<>();
        for (String key : scanKeys(pattern)) {
            Map<Object, Object> fields = redisTemplate.opsForHash().entries(key);
            if (fields.isEmpty()) continue;

            long sum   = parseLong(fields, "sum");
            long count = parseLong(fields, "count");
            if (count == 0) continue;

            double avg = Math.round((double) sum / count * 100.0) / 100.0;
            results.add(YearlyPrice.builder()
                    .productCode(extractProductCode(key))
                    .priceYear(year)
                    .avgPrice(avg)
                    .minPrice(parseLong(fields, "min"))
                    .maxPrice(parseLong(fields, "max"))
                    .priceCount((int) count)
                    .build());
        }
        log.info("Redis 연간 집계 조회: {}건 (year={})", results.size(), year);
        return results;
    }

    // Read: start/end price (Processor에서 호출)
    public Long getWeeklyStartPrice(String productCode, int year, int weekNum) {
        return getHashLong(String.format(AGG_WEEKLY_KEY, productCode, year, weekNum), "first_price");
    }

    public Long getWeeklyEndPrice(String productCode, int year, int weekNum) {
        return getHashLong(String.format(AGG_WEEKLY_KEY, productCode, year, weekNum), "last_price");
    }

    public Long getMonthlyStartPrice(String productCode, int year, int month) {
        return getHashLong(String.format(AGG_MONTHLY_KEY, productCode, year, month), "first_price");
    }

    public Long getMonthlyEndPrice(String productCode, int year, int month) {
        return getHashLong(String.format(AGG_MONTHLY_KEY, productCode, year, month), "last_price");
    }

    public Long getYearlyStartPrice(String productCode, int year) {
        return getHashLong(String.format(AGG_YEARLY_KEY, productCode, year), "first_price");
    }

    public Long getYearlyEndPrice(String productCode, int year) {
        return getHashLong(String.format(AGG_YEARLY_KEY, productCode, year), "last_price");
    }

    /** SCAN으로 패턴에 맞는 키 목록을 반환 (KEYS 명령 사용 금지) */
    private List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                log.error("Redis SCAN 실패: pattern={}", pattern, e);
            }
            return null;
        });
        return keys;
    }

    /** "agg:{type}:{productCode}:{...}" → parts[2] = productCode */
    private String extractProductCode(String key) {
        return key.split(":")[2];
    }

    private Long getHashLong(String key, String field) {
        String val = (String) redisTemplate.opsForHash().get(key, field);
        return val == null ? null : Long.parseLong(val);
    }

    private long parseLong(Map<Object, Object> fields, String field) {
        Object val = fields.get(field);
        return val == null ? 0L : Long.parseLong((String) val);
    }
}
