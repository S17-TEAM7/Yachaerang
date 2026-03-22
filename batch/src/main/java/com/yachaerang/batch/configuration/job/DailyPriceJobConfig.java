package com.yachaerang.batch.configuration.job;

import com.yachaerang.batch.domain.dailyPrice.processor.DailyPriceProcessor;
import com.yachaerang.batch.domain.dailyPrice.reader.DailyPriceReader;
import com.yachaerang.batch.domain.dailyPrice.writer.DailyPriceWriter;
import com.yachaerang.batch.domain.dto.KamisPriceItem;
import com.yachaerang.batch.domain.entity.DailyPrice;
import com.yachaerang.batch.listener.ItemSkipListener;
import com.yachaerang.batch.listener.JobCompletionListener;
import com.yachaerang.batch.listener.MdcStepListener;
import com.yachaerang.batch.listener.StepExecutionListener;
import com.yachaerang.batch.repository.DailyPriceRepository;
import com.yachaerang.batch.repository.ProductRepository;
import com.yachaerang.batch.service.KamisApiService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Daily Price Job에 대한 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyPriceJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final JobCompletionListener jobCompletionListener;
    private final StepExecutionListener stepExecutionListener;
    private final MdcStepListener mdcStepListener;
    private final ItemSkipListener itemSkipListener;

    private final KamisApiService kamisApiService;
    private final ProductRepository productRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final ThreadPoolTaskExecutor partitionTaskExecutor;

    private static final int CHUNK_SIZE = 500;
    private static final List<String> CATEGORY_CODES = List.of("100", "200", "300", "400", "500", "600");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Job: 카테고리
     */
    @Bean
    public Job dailyPriceJob() {
        return new JobBuilder("dailyPriceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionListener)
                .start(dailyPricePartitionStep())
                .build();
    }

    /**
     * Master Step: 카테고리별 파티셔닝
     */
    @Bean
    public Step dailyPricePartitionStep() {
        return new StepBuilder("dailyPricePartitionStep", jobRepository)
                .partitioner("dailyCategoryPartitioner", dailyCategoryPartitioner(null))
                .step(dailyPriceWorkerStep())
                .taskExecutor(partitionTaskExecutor)
                .gridSize(CATEGORY_CODES.size())
                .build();
    }

    /**
     * 카테고리별 Partitioner
     * 각 파티션에 targetDate + categoryCode 설정
     */
    @Bean
    @StepScope
    public Partitioner dailyCategoryPartitioner(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {

        LocalDate targetDate = (targetDateStr != null && !targetDateStr.isBlank())
                ? LocalDate.parse(targetDateStr, FORMATTER)
                : LocalDate.now().minusDays(1);

        return gridSize -> {
            Map<String, ExecutionContext> partitions = new HashMap<>();
            log.info("카테고리 파티셔닝 시작: targetDate={}, categories={}", targetDate, CATEGORY_CODES);

            for (int i = 0; i < CATEGORY_CODES.size(); i++) {
                ExecutionContext context = new ExecutionContext();
                context.putString("targetDate", targetDate.format(FORMATTER));
                context.putString("categoryCode", CATEGORY_CODES.get(i));
                partitions.put("partition" + i, context);
            }

            log.info("총 {} 개의 카테고리 파티션 생성", partitions.size());
            return partitions;
        };
    }

    /**
     * Worker Step: 각 카테고리 파티션 처리
     */
    @Bean
    public Step dailyPriceWorkerStep() {
        return new StepBuilder("dailyPriceWorkerStep", jobRepository)
                .listener(stepExecutionListener)
                .listener(mdcStepListener)
                .<KamisPriceItem, DailyPrice>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(dailyPriceReader(null, null))
                .processor(dailyPriceProcessor(null, null))
                .writer(dailyPriceWriter())
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .listener(itemSkipListener)
                .build();
    }

    /**
     * Reader: stepExecutionContext에서 targetDate, categoryCode 읽기
     */
    @Bean
    @StepScope
    public DailyPriceReader dailyPriceReader(
            @Value("#{stepExecutionContext['targetDate']}") String targetDateStr,
            @Value("#{stepExecutionContext['categoryCode']}") String categoryCode) {

        LocalDate targetDate = LocalDate.parse(targetDateStr, FORMATTER);

        log.info("Reader 생성: targetDate={}, categoryCode={}", targetDate, categoryCode);

        return new DailyPriceReader(kamisApiService, targetDate, categoryCode);
    }

    /**
     * Processor: stepExecutionContext에서 targetDate 읽기
     */
    @Bean
    @StepScope
    public DailyPriceProcessor dailyPriceProcessor(
            @Value("#{stepExecutionContext['targetDate']}") String targetDateStr,
            @Value("#{stepExecutionContext['categoryCode']}") String categoryCode) {

        LocalDate targetDate = LocalDate.parse(targetDateStr, FORMATTER);

        log.info("Processor 생성: targetDate={}, categoryCode={}", targetDate, categoryCode);

        return new DailyPriceProcessor(productRepository, dailyPriceRepository, targetDate);
    }

    /*
    Writer Step 등록
     */
    @Bean
    public DailyPriceWriter dailyPriceWriter() {
        return new DailyPriceWriter(dailyPriceRepository);
    }
}
