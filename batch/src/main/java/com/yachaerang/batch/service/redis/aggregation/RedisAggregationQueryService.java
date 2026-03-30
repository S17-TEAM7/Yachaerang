package com.yachaerang.batch.service.redis.aggregation;

import com.yachaerang.batch.domain.entity.MonthlyPrice;
import com.yachaerang.batch.domain.entity.WeeklyPrice;
import com.yachaerang.batch.domain.entity.YearlyPrice;
import com.yachaerang.batch.service.redis.RedisAggregationWriter;
import com.yachaerang.batch.util.WeekUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Redis 집계 데이터 조회 호출 클래스
 */
@Service
@RequiredArgsConstructor
public class RedisAggregationQueryService {

    private final AggregationLoader          loader;
    private final AggregationKeyFactory      keyFactory;
    private final RedisAggregationFieldReader fieldReader;
    private final RedisAggregationWriter      redisAggregationWriter;

    /**
     * Weekly 집계 데이터를 반환
     */
    public List<WeeklyPrice> getWeeklyAggregatedPrices(LocalDate startDate, LocalDate endDate) {
        long version = redisAggregationWriter.getActiveVersion();
        int[] yw    = WeekUtils.getYearAndWeek(startDate);
        int year    = yw[0];
        int weekNum = yw[1];
        return loader.load(
                keyFactory.weeklyIndexKey(version, year, weekNum),
                String.format("주간(year=%d, week=%d)", year, weekNum),
                pc -> keyFactory.weeklyHashKey(version, pc, year, weekNum),
                new WeeklyPriceMapper(year, weekNum, startDate, endDate, fieldReader));
    }

    /**
     * Monthly 집계 데이터를 반환
     */
    public List<MonthlyPrice> getMonthlyAggregatedPrices(int year, int month) {
        long version = redisAggregationWriter.getActiveVersion();
        return loader.load(
                keyFactory.monthlyIndexKey(version, year, month),
                String.format("월간(year=%d, month=%d)", year, month),
                pc -> keyFactory.monthlyHashKey(version, pc, year, month),
                new MonthlyPriceMapper(year, month, fieldReader));
    }

    /**
     * Yearly 집계 데이터를 반환
     */
    public List<YearlyPrice> getYearlyAggregatedPrices(int year) {
        long version = redisAggregationWriter.getActiveVersion();
        return loader.load(
                keyFactory.yearlyIndexKey(version, year),
                String.format("연간(year=%d)", year),
                pc -> keyFactory.yearlyHashKey(version, pc, year),
                new YearlyPriceMapper(year, fieldReader));
    }


    public Long getWeeklyStartPrice(String productCode, int year, int weekNum) {
        long version = redisAggregationWriter.getActiveVersion();
        return fieldReader.getHashLong(keyFactory.weeklyHashKey(version, productCode, year, weekNum), "first_price");
    }

    public Long getWeeklyEndPrice(String productCode, int year, int weekNum) {
        long version = redisAggregationWriter.getActiveVersion();
        return fieldReader.getHashLong(keyFactory.weeklyHashKey(version, productCode, year, weekNum), "last_price");
    }

    public Long getMonthlyStartPrice(String productCode, int year, int month) {
        long version = redisAggregationWriter.getActiveVersion();
        return fieldReader.getHashLong(keyFactory.monthlyHashKey(version, productCode, year, month), "first_price");
    }

    public Long getMonthlyEndPrice(String productCode, int year, int month) {
        long version = redisAggregationWriter.getActiveVersion();
        return fieldReader.getHashLong(keyFactory.monthlyHashKey(version, productCode, year, month), "last_price");
    }

    public Long getYearlyStartPrice(String productCode, int year) {
        long version = redisAggregationWriter.getActiveVersion();
        return fieldReader.getHashLong(keyFactory.yearlyHashKey(version, productCode, year), "first_price");
    }

    public Long getYearlyEndPrice(String productCode, int year) {
        long version = redisAggregationWriter.getActiveVersion();
        return fieldReader.getHashLong(keyFactory.yearlyHashKey(version, productCode, year), "last_price");
    }
}
