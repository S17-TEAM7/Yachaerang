package com.yachaerang.batch.configuration.job;

import com.yachaerang.batch.domain.dailyPrice.processor.DailyPriceProcessor;
import com.yachaerang.batch.domain.dailyPrice.reader.DailyPriceReader;
import com.yachaerang.batch.domain.dailyPrice.writer.DailyPriceWriter;
import com.yachaerang.batch.domain.dto.KamisPriceItem;
import com.yachaerang.batch.domain.entity.DailyPrice;
import com.yachaerang.batch.listener.ItemSkipListener;
import com.yachaerang.batch.listener.JobCompletionListener;
import com.yachaerang.batch.listener.MdcStepListener;
import com.yachaerang.batch.listener.RedisVersionStepListener;
import com.yachaerang.batch.listener.StepExecutionListener;
import com.yachaerang.batch.repository.DailyPriceRepository;
import com.yachaerang.batch.repository.ProductRepository;
import com.yachaerang.batch.service.external.KamisApiService;
import com.yachaerang.batch.service.redis.RedisAggregationWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
특정 기간동안의 DailyPrice 수집 Job Configuration
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class DateRangePriceJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final JobCompletionListener jobCompletionListener;
    private final StepExecutionListener stepExecutionListener;
    private final MdcStepListener mdcStepListener;
    private final ItemSkipListener itemSkipListener;

    private final KamisApiService kamisApiService;
    private final ProductRepository productRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final RedisAggregationWriter redisAggregationWriter;
    private final RedisVersionStepListener redisVersionStepListener;
    @Qualifier("partitionTaskExecutor")
    private final ThreadPoolTaskExecutor partitionTaskExecutor;

    private static final int CHUNK_SIZE = 100;
    private static final List<String> CATEGORY_CODES = List.of("100", "200", "300", "400", "500", "600");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /*
    기간 범위 일별 가격 수집 Job
     */
    @Bean
    public Job dateRangePriceJob() {
        return new JobBuilder("dateRangePriceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionListener)
                .start(dateRangePartitionStep())
                .next(dateRangeRedisAggregationStep())
                .build();
    }

    /*
    Partitioning 가장 상위의 Step
     */
    @Bean
    public Step dateRangePartitionStep() {
        return new StepBuilder("dateRangePartitionStep", jobRepository)
                .partitioner("dailyStepPartitioner", dateRangePartitioner(null, null))
                .step(partitionedPriceStep())
                .taskExecutor(partitionTaskExecutor)
                .gridSize(CATEGORY_CODES.size())
                .build();
    }

    /*
    날짜 범위 기반의 Partitioner
     */
    @Bean
    @StepScope
    public Partitioner dateRangePartitioner(
            @Value("#{jobParameters['startDate']}") String startDateStr,
            @Value("#{jobParameters['endDate']}") String endDateStr
    ) {
        return gridSize -> {
            Map<String, ExecutionContext> partitions = new HashMap<>();
            LocalDate startDate = LocalDate.parse(startDateStr, FORMATTER);
            LocalDate endDate = LocalDate.parse(endDateStr, FORMATTER);
            log.info("Start Partitioning: {} ~ {}, categories={}", startDate, endDate, CATEGORY_CODES);

            LocalDate currentDate = startDate;
            int partitionNumber = 0;

            // endDate까지 날짜 × 카테고리 파티션 생성
            while (!currentDate.isAfter(endDate)) {
                for (String categoryCode : CATEGORY_CODES) {
                    ExecutionContext context = new ExecutionContext();
                    context.putString("targetDate", currentDate.format(FORMATTER));
                    context.putString("categoryCode", categoryCode);
                    context.putInt("partitionNumber", partitionNumber);
                    partitions.put("partition" + partitionNumber, context);
                    partitionNumber++;
                }
                currentDate = currentDate.plusDays(1);
            }
            log.info("Total {} 개의 Partitions 생성 (날짜 × 카테고리)", partitions.size());
            return partitions;
        };
    }

    /*
    Partition의 개별 날짜에 대한 처리 Step
     */
    @Bean
    public Step partitionedPriceStep() {
        return new StepBuilder("partitionedPriceStep", jobRepository)
                .<KamisPriceItem, DailyPrice>chunk(CHUNK_SIZE, platformTransactionManager)
                .listener(stepExecutionListener)
                .listener(mdcStepListener)
                .reader(partitionedPriceReader(null, null))
                .processor(partitionedPriceProcessor(null))
                .writer(partitionedPriceWriter())
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .listener(itemSkipListener)
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /*
    Partitioner에 대한 Reader
     */
    @Bean
    @StepScope
    public DailyPriceReader partitionedPriceReader(
            @Value("#{stepExecutionContext['targetDate']}") String targetDateStr,
            @Value("#{stepExecutionContext['categoryCode']}") String categoryCode) {

        LocalDate targetDate = LocalDate.parse(targetDateStr);

        log.info("PartitionedReader 생성: targetDate={}, categoryCode={}", targetDate, categoryCode);

        return new DailyPriceReader(kamisApiService, targetDate, categoryCode);
    }

    /*
    Partitioner에 대한 Processor
     */
    @Bean
    @StepScope
    public DailyPriceProcessor partitionedPriceProcessor(
            @Value("#{stepExecutionContext['targetDate']}") String targetDateStr) {

        LocalDate targetDate = LocalDate.parse(targetDateStr);

        log.info("PartitionedProcessor 생성: targetDate={}", targetDate);

        return new DailyPriceProcessor(productRepository, dailyPriceRepository, targetDate);
    }

    @Bean
    public DailyPriceWriter partitionedPriceWriter() {
        return new DailyPriceWriter(dailyPriceRepository);
    }

    /**
     * Redis 집계 Step: DB에 실제 저장된 데이터를 기준으로 Redis를 갱신
     */
    @Bean
    @StepScope
    public ItemReader<DailyPrice> dateRangeRedisAggregationReader(
            @Value("#{jobParameters['startDate']}") String startDateStr,
            @Value("#{jobParameters['endDate']}") String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr, FORMATTER);
        LocalDate endDate = LocalDate.parse(endDateStr, FORMATTER);
        return new ItemReader<>() {
            private int offset = 0;
            private List<DailyPrice> buffer = Collections.emptyList();
            private int bufferIndex = 0;

            @Override
            public DailyPrice read() {
                if (bufferIndex >= buffer.size()) {
                    buffer = dailyPriceRepository.findByPriceDateBetweenPaged(startDate, endDate, offset, CHUNK_SIZE);
                    offset += buffer.size();
                    bufferIndex = 0;
                    if (buffer.isEmpty()) {
                        log.info("Redis 집계 Reader 완료: {} ~ {}, 총 {} 건", startDate, endDate, offset);
                        return null;
                    }
                    log.debug("Redis 집계 Reader 페이지 로드: offset={}, size={}", offset - buffer.size(), buffer.size());
                }
                return buffer.get(bufferIndex++);
            }
        };
    }

    @Bean
    public Step dateRangeRedisAggregationStep() {
        return new StepBuilder("dateRangeRedisAggregationStep", jobRepository)
                .<DailyPrice, DailyPrice>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(dateRangeRedisAggregationReader(null, null))
                .writer(chunk -> chunk.getItems().forEach(redisAggregationWriter::updateAggregations))
                .listener(redisVersionStepListener)
                .build();
    }
}
