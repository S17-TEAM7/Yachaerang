package com.yachaerang.batch.configuration.job;

import com.yachaerang.batch.configuration.parameter.JobPeriodParameter;
import com.yachaerang.batch.domain.entity.YearlyPrice;
import com.yachaerang.batch.listener.ItemSkipListener;
import com.yachaerang.batch.listener.JobCompletionListener;
import com.yachaerang.batch.listener.MdcStepListener;
import com.yachaerang.batch.listener.StepExecutionListener;
import com.yachaerang.batch.service.aggregation.YearlyPriceAggregationService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class YearlyPriceJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final JobCompletionListener jobCompletionListener;
    private final StepExecutionListener stepExecutionListener;
    private final MdcStepListener mdcStepListener;
    private final ItemSkipListener itemSkipListener;

    private final YearlyPriceAggregationService yearlyPriceAggregationService;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job yearlyPriceJob(Step yearlyPriceStep) {
        return new JobBuilder("yearlyPriceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionListener)
                .start(yearlyPriceStep)
                .build();
    }

    /*
    YearlyPrice Step
     */
    @Bean
    public Step yearlyPriceStep(
            ListItemReader<YearlyPrice> yearlyPriceReader,
            ItemProcessor<YearlyPrice, YearlyPrice> yearlyPriceProcessor,
            ItemWriter<YearlyPrice> yearlyPriceWriter
    ) {
        return new StepBuilder("yearlyPriceStep", jobRepository)
                .<YearlyPrice, YearlyPrice>chunk(CHUNK_SIZE, platformTransactionManager)
                .listener(stepExecutionListener)
                .listener(mdcStepListener)
                .reader(yearlyPriceReader)
                .processor(yearlyPriceProcessor)
                .writer(yearlyPriceWriter)
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
     * @param : 대상 년도
     * @return
     */
    @Bean
    @StepScope
    public ListItemReader<YearlyPrice> yearlyPriceReader(JobPeriodParameter jobPeriodParameter) {
        int year = jobPeriodParameter.getYear();

        List<YearlyPrice> yearlyPriceList =
                yearlyPriceAggregationService.getYearlyAggregatedPrices(year);

        log.info("ListItemReader 초기화 - 최종 리스트 크기: {}", yearlyPriceList.size());
        return new ListItemReader<>(yearlyPriceList);
    }

    /**
     * Processor Step : start_price, end_price 설정해 저장 + 변화율 저장
     * @return
     */
    @Bean
    @StepScope
    public ItemProcessor<YearlyPrice, YearlyPrice> yearlyPriceProcessor(JobPeriodParameter jobPeriodParameter) {
        int year = jobPeriodParameter.getYear();

        return item -> {
            if (item.getPriceCount() == 0) {
                log.debug("priceCount가 0이므로 스킵: {}", item.getProductCode());
                return null;
            }

            // 연초와 연말 가격 설정
            Long startPrice = yearlyPriceAggregationService.getStartPrice(item.getProductCode(), year);
            Long endPrice = yearlyPriceAggregationService.getEndPrice(item.getProductCode(), year);
            item.setStartPrice(startPrice);
            item.setEndPrice(endPrice);

            // 가격 변화 계산
            Long priceChange = 0L;
            BigDecimal priceChangeRate = BigDecimal.ZERO;

            // 둘 다 null 체크 + startPrice로 나누기
            if (startPrice != null && endPrice != null && startPrice > 0) {
                priceChange = endPrice - startPrice;  // 연말 - 연초
                priceChangeRate = BigDecimal.valueOf(priceChange)
                        .divide(BigDecimal.valueOf(startPrice), 4, RoundingMode.HALF_UP)  // startPrice로 나누기
                        .multiply(BigDecimal.valueOf(100));

                log.debug("연간 변화 계산: productCode={}, start={}, end={}, change={}, rate={}%",
                        item.getProductCode(), startPrice, endPrice, priceChange, priceChangeRate);
            } else {
                log.debug("연간 변화 계산 불가: productCode={}, startPrice={}, endPrice={}",
                        item.getProductCode(), startPrice, endPrice);
            }

            item.setPriceChange(priceChange);
            item.setPriceChangeRate(priceChangeRate);

            return item;
        };
    }

    /**
     * Writer 스텝
     *
     * @return
     */
    @Bean
    public ItemWriter<YearlyPrice> yearlyPriceWriter() {
        return chunk -> {
            List<YearlyPrice> items = new ArrayList<>(chunk.getItems());
            log.info("저장할 데이터 : {} 건", items.size());
            yearlyPriceAggregationService.saveYearlyPrices(items);
        };
    }
}