package com.yachaerang.batch.repository;

import com.yachaerang.batch.domain.entity.WeeklyPrice;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WeeklyPriceRepository {

    /*
    주간 가격 데이터 저장
     */
    void upsertWeeklyPrice(WeeklyPrice weeklyPrice);

    /*
    배치 저장
     */
    void batchUpsertWeeklyPrice(List<WeeklyPrice> weeklyPrices);
}
