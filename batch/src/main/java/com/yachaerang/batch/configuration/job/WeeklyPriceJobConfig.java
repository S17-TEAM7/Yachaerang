package com.yachaerang.batch.configuration.job;

import com.yachaerang.batch.configuration.parameter.JobPeriodParameter;
import com.yachaerang.batch.domain.entity.WeeklyPrice;
import com.yachaerang.batch.domain.processor.WeeklyPriceProcessor;
import com.yachaerang.batch.listener.ItemSkipListener;
import com.yachaerang.batch.listener.JobCompletionListener;
import com.yachaerang.batch.listener.MdcStepListener;
import com.yachaerang.batch.listener.StepExecutionListener;
import com.yachaerang.batch.service.redis.aggregation.RedisAggregationQueryService;
import com.yachaerang.batch.service.aggregation.WeeklyPriceAggregationService;
import com.yachaerang.batch.util.WeekUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WeeklyPriceJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final JobCompletionListener jobCompletionListener;
    private final StepExecutionListener stepExecutionListener;
    private final MdcStepListener mdcStepListener;
    private final ItemSkipListener itemSkipListener;

    private final WeeklyPriceAggregationService weeklyPriceAggregationService;
    private final RedisAggregationQueryService redisAggregationQueryService;

    private static final int CHUNK_SIZE= 100;

    @Bean
    public Job weeklyPriceJob() {
        return new JobBuilder("weeklyPriceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionListener)
                .start(weeklyPriceStep())
                .build();
    }

    /*
    WeeklyPrice Step
     */
    @Bean
    public Step weeklyPriceStep() {
        return new StepBuilder("weeklyPriceStep", jobRepository)
                .<WeeklyPrice, WeeklyPrice>chunk(CHUNK_SIZE, platformTransactionManager)
                .listener(stepExecutionListener)
                .listener(mdcStepListener)
                .reader(weeklyPriceReader(null))
                .processor(weeklyPriceProcessor())
                .writer(weeklyPriceWriter())
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .listener(itemSkipListener)
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * 집계 일기
     * @param jobPeriodParameter
     * @return
     */
    @Bean
    @StepScope
    public ListItemReader<WeeklyPrice> weeklyPriceReader(JobPeriodParameter jobPeriodParameter) {

        int year = jobPeriodParameter.getYear();
        int week = jobPeriodParameter.getWeek();
        LocalDate startDate = WeekUtils.getWeekStartDate(year, week);
        LocalDate endDate = WeekUtils.getWeekEndDate(year, week);

        List<WeeklyPrice> weeklyPriceList =
                weeklyPriceAggregationService.getWeeklyAggregatedPrices(startDate, endDate);

        log.info("ListItemReader 초기화 - 최종 리스트 크기: {}", weeklyPriceList.size());

        return new ListItemReader<>(weeklyPriceList);
    }

    /**
     * Processor 스텝
     * 데이터 검증 및 가공
     * @return
     */
    @Bean
    @StepScope
    public WeeklyPriceProcessor weeklyPriceProcessor() {

        return new WeeklyPriceProcessor(redisAggregationQueryService);
    }

    /**
     * Writer 스텝
     * 집계 서비스를 통해 대신 MyBatis 실행
     * @return
     */
    @Bean
    public ItemWriter<WeeklyPrice> weeklyPriceWriter() {
        return chunk -> {
            List<WeeklyPrice> items = new ArrayList<>(chunk.getItems());
            log.info("저장할 데이터 : {} 건", items.size());
            weeklyPriceAggregationService.saveWeeklyPrices(items);
        };
    }
}
