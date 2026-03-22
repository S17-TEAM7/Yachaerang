package com.yachaerang.batch.configuration.job;

import com.yachaerang.batch.domain.entity.DailyPrice;
import com.yachaerang.batch.listener.JobCompletionListener;
import com.yachaerang.batch.repository.DailyPriceRepository;
import com.yachaerang.batch.service.RedisAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.List;

/**
 * daily_price 테이블 전체 데이터를 Redis에 일괄 반영하는 초기화 Job
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisAggregationInitJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final JobCompletionListener jobCompletionListener;
    private final DailyPriceRepository dailyPriceRepository;
    private final RedisAggregationService redisAggregationService;

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job redisAggregationInitJob(Step redisAggregationInitStep) {
        return new JobBuilder("redisAggregationInitJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionListener)
                .start(redisAggregationInitStep)
                .build();
    }

    @Bean
    public Step redisAggregationInitStep() {
        return new StepBuilder("redisAggregationInitStep", jobRepository)
                .<DailyPrice, DailyPrice>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(dailyPriceInitReader())
                .writer(redisAggregationInitWriter())
                .build();
    }

    /*
    daily_price 전체를 CHUNK_SIZE 단위로 페이징 조회하는 Reader
     */
    @Bean
    public ItemReader<DailyPrice> dailyPriceInitReader() {
        return new ItemReader<>() {
            private int offset = 0;
            private List<DailyPrice> buffer = Collections.emptyList();
            private int bufferIndex = 0;

            @Override
            public DailyPrice read() {
                if (bufferIndex >= buffer.size()) {
                    buffer = dailyPriceRepository.findAllPaged(offset, CHUNK_SIZE);
                    offset += buffer.size();
                    bufferIndex = 0;
                    if (buffer.isEmpty()) {
                        log.info("Redis 초기화 Reader 완료: 총 {} 건 처리", offset);
                        return null;
                    }
                    log.debug("Redis 초기화 Reader 페이지 로드: offset={}, size={}", offset - buffer.size(), buffer.size());
                }
                return buffer.get(bufferIndex++);
            }
        };
    }

    /*
    각 DailyPrice 에 대해 Redis 집계 업데이트
     */
    @Bean
    public ItemWriter<DailyPrice> redisAggregationInitWriter() {
        return chunk -> {
            List<? extends DailyPrice> items = chunk.getItems();
            items.forEach(redisAggregationService::updateAggregations);
            log.debug("Redis 집계 업데이트: {} 건", items.size());
        };
    }
}
