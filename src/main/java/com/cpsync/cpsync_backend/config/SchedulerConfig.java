package com.cpsync.cpsync_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskExecutor ioTaskExecutor(
            @Value("${app.sync.thread-pool-size:10}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("io-task-");
        executor.initialize();
        return executor;
    }
}