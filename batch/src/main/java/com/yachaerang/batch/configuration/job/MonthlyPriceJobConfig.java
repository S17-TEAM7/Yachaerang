package com.yachaerang.batch.configuration.job;

import com.yachaerang.batch.configuration.parameter.JobPeriodParameter;
import com.yachaerang.batch.domain.entity.MonthlyPrice;
import com.yachaerang.batch.domain.processor.MonthlyPriceProcessor;
import com.yachaerang.batch.listener.ItemSkipListener;
import com.yachaerang.batch.listener.JobCompletionListener;
import com.yachaerang.batch.listener.MdcStepListener;
import com.yachaerang.batch.listener.StepExecutionListener;
import com.yachaerang.batch.repository.DailyPriceRepository;
import com.yachaerang.batch.service.MonthlyPriceAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyPriceJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final JobCompletionListener jobCompletionListener;
    private final StepExecutionListener stepExecutionListener;
    private final MdcStepListener mdcStepListener;
    private final ItemSkipListener itemSkipListener;

    private final MonthlyPriceAggregationService monthlyPriceAggregationService;
    private final DailyPriceRepository dailyPriceRepository;


    private static final int CHUNK_SIZE= 100;

    @Bean
    public Job monthlyPriceJob(Step monthlyPriceStep) {
        return new JobBuilder("monthlyPriceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionListener)
                .start(monthlyPriceStep)
                .build();
    }

    /*
    MonthlyPrice Step
     */
    @Bean
    public Step monthlyPriceStep(
            ListItemReader<MonthlyPrice> monthlyPriceReader,
            ItemProcessor<MonthlyPrice, MonthlyPrice> monthlyPriceProcessor,
            ItemWriter<MonthlyPrice> monthlyPriceWriter
    ) {
        return new StepBuilder("monthlyPriceStep", jobRepository)
                .<MonthlyPrice, MonthlyPrice>chunk(CHUNK_SIZE, platformTransactionManager)
                .listener(stepExecutionListener)
                .listener(mdcStepListener)
                .reader(monthlyPriceReader)
                .processor(monthlyPriceProcessor())
                .writer(monthlyPriceWriter)
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .listener(itemSkipListener)
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * Reader 스텝
     * @param jobPeriodParameter: 연도와 월
     * @return
     */
    @Bean
    @StepScope
    public ListItemReader<MonthlyPrice> monthlyPriceReader(JobPeriodParameter jobPeriodParameter) {
        int year = jobPeriodParameter.getYear();
        int month = jobPeriodParameter.getMonth();

        List<MonthlyPrice> monthlyPriceList =
                monthlyPriceAggregationService.getMonthlyAggregatedPrices(year, month);

        log.info("ListItemReader 초기화 - 최종 리스트 크기: {}", monthlyPriceList.size());
        return new ListItemReader<>(monthlyPriceList);
    }

    /**
     * Processor Step
     * @return
     */
    @Bean
    @StepScope
    public MonthlyPriceProcessor monthlyPriceProcessor() {
        return new MonthlyPriceProcessor(dailyPriceRepository);
    }

    /**
     * Writer 스텝
     * 집계 서비스를 통해 대신 MyBatis 실행
     * @return
     */
    @Bean
    public ItemWriter<MonthlyPrice> monthlyPriceWriter() {
        return chunk -> {
            List<MonthlyPrice> items = new ArrayList<>(chunk.getItems());
            log.info("저장할 데이터 : {} 건", items.size());
            monthlyPriceAggregationService.saveMonthlyPrices(items);
        };
    }
}
