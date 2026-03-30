package com.yachaerang.batch.repository;

import com.yachaerang.batch.domain.entity.DailyPrice;
import com.yachaerang.batch.domain.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyPriceRepository {

    /*
    가장 최근의 해당 품목의 가격 조회
     */
    Long findLatestPriceByProductCode(
            @Param("productCode") String productCode,
            @Param("priceDate") LocalDate priceDate
    );

    /*
    전부 저장하기
     */
    int saveAll(List<DailyPrice> dailyPrices);

    /*
    해당 상품과 날짜에 대해 존재여부 확인
     */
    boolean existsByProductAndPriceDate(
            @Param("product")Product product,
            @Param("priceDate") LocalDate priceDate
            );

    // 주간에 사용
    /**
     * 특정 상품의 주차 내 가장 빠른 날짜의 가격 조회
     * @param weekStartDate : 관심 주차의 시작일
     * @param weekEndDate : 관심 주차의 종료일
     * @return : 가격
     */
    Long findEarliestPriceInWeek(
            @Param("productCode") String productCode,
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("weekEndDate") LocalDate weekEndDate
    );

    /**
     * 특정 상품의 주차 내 가장 마지막 날자의 가격 조회
     * @param weekStartDate : 관심 주차의 시작일
     * @param weekEndDate : 관심 주차의 종료일
     * @return : 가격
     */
    Long findLatestPriceInWeek(
            @Param("productCode") String productCode,
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("weekEndDate") LocalDate weekEndDate
    );

    /**
     * 특정 달에서의 가장 빠른 날의 가격 조회
     * @param productCode : 대상 상품
     * @param monthStartDate : 특정 달의 시작일자
     * @param monthEndDate : 특정 달의 종료일자
     * @return : 가격
     */
    Long findEarliestPriceInMonth(@Param("productCode") String productCode,
                                     @Param("monthStartDate") LocalDate monthStartDate,
                                     @Param("monthEndDate") LocalDate monthEndDate);

    /**
     * 특정 달에서의 가장 마지막 날의 가격 조회
     * @param productCode : 대상 상품
     * @param monthStartDate : 특정 달의 시작일자
     * @param monthEndDate : 특정 달의 종료일자
     * @return : 가격
     */
    Long findLatestPriceInMonth(@Param("productCode") String productCode,
                                   @Param("monthStartDate") LocalDate monthStartDate,
                                   @Param("monthEndDate") LocalDate monthEndDate);

    /*
    Redis 초기화용 전체 조회 (페이징)
     */
    List<DailyPrice> findAllPaged(@Param("offset") int offset, @Param("limit") int limit);

    /*
    날짜 범위 내 데이터 조회 (페이징) - Redis 집계 Step 전용
     */
    List<DailyPrice> findByPriceDateBetweenPaged(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
}
