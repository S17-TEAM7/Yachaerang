package com.yachaerang.batch.repository;

import com.yachaerang.batch.domain.entity.MonthlyPrice;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MonthlyPriceRepository {

    /*
    한달에 대한 가격 저장
     */
    void upsertMonthlyPrice(MonthlyPrice monthlyPrice);

    /*
    배치 저장
     */
    void batchUpsertMonthlyPrice(List<MonthlyPrice> monthlyPrices);
}
