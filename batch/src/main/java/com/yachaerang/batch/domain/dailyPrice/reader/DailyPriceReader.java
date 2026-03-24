package com.yachaerang.batch.domain.dailyPrice.reader;

import com.yachaerang.batch.domain.dto.KamisPriceItem;
import com.yachaerang.batch.service.external.KamisApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
Daily Price Job의 Reader
 */
@Slf4j
public class DailyPriceReader implements ItemReader<KamisPriceItem> {

    private final KamisApiService kamisApiService;
    private final LocalDate targetDate;
    private final Queue<String> categoryCodeQueue;

    private Iterator<KamisPriceItem> currentIterator;

    public DailyPriceReader(KamisApiService kamisApiService,
                            LocalDate targetDate,
                            List<String> categoryCodes) {
        this.kamisApiService = kamisApiService;
        this.targetDate = targetDate;
        this.categoryCodeQueue = new LinkedList<>(categoryCodes);
        this.currentIterator = null;

        log.info("DailyPriceReader 생성: date={}, categories={}", targetDate, categoryCodes);
    }

    public DailyPriceReader(KamisApiService kamisApiService,
                            LocalDate targetDate,
                            String categoryCode) {
        this(kamisApiService, targetDate, List.of(categoryCode));
    }

    /*
    API 요청에 대하여 읽기 -> DTO 반환
     */
    @Override
    public KamisPriceItem read() {
        // 현재 Iterator에 데이터가 남아있으면 반환
        if (currentIterator != null && currentIterator.hasNext()) {
            return currentIterator.next();
        }

        // 다음 카테고리로 이동
        while (!categoryCodeQueue.isEmpty()) {
            String categoryCode = categoryCodeQueue.poll();
            log.info("카테고리 {} 데이터 조회 시작: {}", categoryCode, targetDate);

            try {
                List<KamisPriceItem> items = kamisApiService.getDailyPrices(targetDate, categoryCode);

                if (items != null && !items.isEmpty()) {
                    currentIterator = items.iterator();
                    log.info("카테고리 {} 에서 {} 건 조회 완료", categoryCode, items.size());
                    return currentIterator.next();
                } else {
                    log.info("카테고리 {} 데이터 없음", categoryCode);
                }
            } catch (Exception e) {
                log.error("카테고리 {} 조회 실패: {}", categoryCode, e.getMessage());
            }
        }
        // 모든 카테고리 처리 완료
        log.info("모든 카테고리 처리 완료: {}", targetDate);
        return null;
    }
}
