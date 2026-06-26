package com.cpsync.cpsync_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskExecutor ioTaskExecutor(
            @Value("${app.sync.thread-pool-size:4}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("io-task-");
        // FIXED: CallerRunsPolicy — if queue is full, the calling thread runs the task.
        // Prevents RejectedExecutionException during large scheduler batches.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}