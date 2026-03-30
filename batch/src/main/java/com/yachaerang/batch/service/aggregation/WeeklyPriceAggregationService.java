package com.yachaerang.batch.service.aggregation;

import com.yachaerang.batch.domain.entity.WeeklyPrice;
import com.yachaerang.batch.repository.WeeklyPriceRepository;
import com.yachaerang.batch.service.redis.aggregation.RedisAggregationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/*
가격 정보 집계를 위한 작업 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WeeklyPriceAggregationService {

    private final WeeklyPriceRepository weeklyPriceRepository;
    private final RedisAggregationQueryService redisAggregationQueryService;

    /**
     * 주간 집계 데이터 조회
     */
    public List<WeeklyPrice> getWeeklyAggregatedPrices(LocalDate startDate, LocalDate endDate) {
        log.info("주간 집계 데이터 조회 - 기간: {} ~ {}", startDate, endDate);
        List<WeeklyPrice> results = redisAggregationQueryService.getWeeklyAggregatedPrices(startDate, endDate);
        log.info("조회된 집계 데이터: {} 건", results.size());
        return results;
    }

    /**
     * 주간 가격 데이터 배치 저장
     */
    public void saveWeeklyPrices(List<WeeklyPrice> weeklyPrices) {
        if (weeklyPrices.isEmpty()) {
            log.info("저장할 데이터가 없습니다");
            return;
        }

        weeklyPriceRepository.batchUpsertWeeklyPrice(weeklyPrices);
        log.info("{} 건 저장 완료", weeklyPrices.size());
    }
}
