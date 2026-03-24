package com.yachaerang.batch.service.redis.aggregation;

import com.yachaerang.batch.domain.entity.MonthlyPrice;
import com.yachaerang.batch.domain.entity.WeeklyPrice;
import com.yachaerang.batch.domain.entity.YearlyPrice;
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

    private final AggregationLoader       loader;
    private final AggregationKeyFactory   keyFactory;
    private final RedisAggregationFieldReader fieldReader;

    /**
     * Weekly 집계 데이터를 반환
     * @param startDate : 시작일
     * @param endDate : 종료일
     * @return : WeeklyPrice 집계 처리 리스트 반환
     */
    public List<WeeklyPrice> getWeeklyAggregatedPrices(LocalDate startDate, LocalDate endDate) {
        int[] yw    = WeekUtils.getYearAndWeek(startDate);
        int year    = yw[0];
        int weekNum = yw[1];
        return loader.load(
                keyFactory.weeklyIndexKey(year, weekNum),
                String.format("주간(year=%d, week=%d)", year, weekNum),
                pc -> keyFactory.weeklyHashKey(pc, year, weekNum),
                new WeeklyPriceMapper(year, weekNum, startDate, endDate, fieldReader));
    }

    /**
     * Monthly 집계 데이터를 반환
     * @param year : 연도
     * @param month : 달
     * @return : MonthlyPrice 집계 리스트 반환
     */
    public List<MonthlyPrice> getMonthlyAggregatedPrices(int year, int month) {
        return loader.load(
                keyFactory.monthlyIndexKey(year, month),
                String.format("월간(year=%d, month=%d)", year, month),
                pc -> keyFactory.monthlyHashKey(pc, year, month),
                new MonthlyPriceMapper(year, month, fieldReader));
    }

    /**
     * Yearly 집계 데이터를 반환
     * @param year : 원하는 대상의 연도
     * @return : 해당 연도에 맞게 처리된 YearlyPrice 리스트
     */
    public List<YearlyPrice> getYearlyAggregatedPrices(int year) {
        return loader.load(
                keyFactory.yearlyIndexKey(year),
                String.format("연간(year=%d)", year),
                pc -> keyFactory.yearlyHashKey(pc, year),
                new YearlyPriceMapper(year, fieldReader));
    }


    // WeeklyPriceProcessor에서 호출
    public Long getWeeklyStartPrice(String productCode, int year, int weekNum) {
        return fieldReader.getHashLong(keyFactory.weeklyHashKey(productCode, year, weekNum), "first_price");
    }

    public Long getWeeklyEndPrice(String productCode, int year, int weekNum) {
        return fieldReader.getHashLong(keyFactory.weeklyHashKey(productCode, year, weekNum), "last_price");
    }

    // MonthlyPriceProcessor 호출
    public Long getMonthlyStartPrice(String productCode, int year, int month) {
        return fieldReader.getHashLong(keyFactory.monthlyHashKey(productCode, year, month), "first_price");
    }

    public Long getMonthlyEndPrice(String productCode, int year, int month) {
        return fieldReader.getHashLong(keyFactory.monthlyHashKey(productCode, year, month), "last_price");
    }

    // YearlyPriceProcessor 호출
    public Long getYearlyStartPrice(String productCode, int year) {
        return fieldReader.getHashLong(keyFactory.yearlyHashKey(productCode, year), "first_price");
    }

    public Long getYearlyEndPrice(String productCode, int year) {
        return fieldReader.getHashLong(keyFactory.yearlyHashKey(productCode, year), "last_price");
    }
}
