package com.yachaerang.batch.repository;

import com.yachaerang.batch.domain.entity.YearlyPrice;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface YearlyPriceRepository {

    /*
    연간 가격 데이터 저장
     */
    int upsertYearlyPrice(YearlyPrice yearlyPrice);
}
