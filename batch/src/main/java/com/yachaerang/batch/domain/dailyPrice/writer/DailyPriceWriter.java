package com.yachaerang.batch.domain.dailyPrice.writer;

import com.yachaerang.batch.domain.entity.DailyPrice;
import com.yachaerang.batch.repository.DailyPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

/*
Daily Price Job의 Writer
 */
@Slf4j
@RequiredArgsConstructor
public class DailyPriceWriter implements ItemWriter<DailyPrice> {

    private final DailyPriceRepository dailyPriceRepository;

    /*
    DB write
     */
    @Override
    public void write(Chunk<? extends DailyPrice> chunk) {
        log.info("일별 가격 데이터 저장: {} 건", chunk.size());

        List<DailyPrice> items = new ArrayList<>(chunk.getItems());
        int inserted = dailyPriceRepository.saveAll(items);

        if (inserted != chunk.size()) {
            log.warn("일부 insert 실패: {} / {}", inserted, chunk.size());
        }
    }
}
