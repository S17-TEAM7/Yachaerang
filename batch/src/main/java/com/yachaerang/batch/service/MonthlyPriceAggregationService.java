package com.yachaerang.batch.service;

import com.yachaerang.batch.domain.entity.MonthlyPrice;
import com.yachaerang.batch.repository.MonthlyPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MonthlyPriceAggregationService {

    private final MonthlyPriceRepository monthlyPriceRepository;
    private final RedisAggregationService redisAggregationService;

    /**
     * 월간 집계 데이터 조회
     */
    public List<MonthlyPrice> getMonthlyAggregatedPrices(int year, int month) {
        log.info("월간 집계 데이터 조회 - 기간: {}년 {}월", year, month);
        List<MonthlyPrice> results = redisAggregationService.getMonthlyAggregatedPrices(year, month);
        log.info("조회된 집계 데이터: {} 건", results.size());
        return results;
    }

    /**
     * 월간 가격 데이터 저장
     */
    public void saveMonthlyPrices(MonthlyPrice monthlyPrice) {
        monthlyPriceRepository.upsertMonthlyPrice(monthlyPrice);
    }

    /**
     * 월간 가격 데이터 배치 저장
     */
    public void saveMonthlyPrices(List<MonthlyPrice> monthlyPrices) {
        if (monthlyPrices == null || monthlyPrices.isEmpty()) {
            log.info("저장할 데이터가 없습니다.");
            return;
        }
        monthlyPriceRepository.batchUpsertMonthlyPrice(monthlyPrices);
        log.info("{} 건 저장 완료",  monthlyPrices.size());
    }
}
