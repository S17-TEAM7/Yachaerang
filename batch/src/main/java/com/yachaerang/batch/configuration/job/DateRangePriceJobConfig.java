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
}
