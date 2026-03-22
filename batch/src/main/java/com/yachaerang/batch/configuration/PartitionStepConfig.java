package com.yachaerang.batch.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionException;

@Configuration
public class PartitionStepConfig {

    private static final int PARTITION_CORE_POOL_SIZE = 10;
    private static final int PARTITION_MAX_POOL_SIZE = 20;
    private static final int PARTITION_QUEUE_CAPACITY = 100;

    @Bean
    public ThreadPoolTaskExecutor partitionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(PARTITION_CORE_POOL_SIZE);
        executor.setMaxPoolSize(PARTITION_MAX_POOL_SIZE);
        executor.setQueueCapacity(PARTITION_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("partition-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler((runnable, pool) -> {
            try {
                pool.getQueue().put(runnable);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("파티션 큐 대기 중 인터럽트 발생", e);
            }
        });
        executor.initialize();
        return executor;
    }
}
