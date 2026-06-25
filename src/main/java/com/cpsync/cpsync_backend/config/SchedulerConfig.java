package com.cpsync.cpsync_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SchedulerConfig {

    /**
     * On Render free tier (0.1 CPU shared), 4 threads is the sweet spot.
     * More threads = more context-switch overhead with no extra throughput.
     * If you upgrade to a paid instance (1+ CPU), raise this to 8-10.
     */
    @Bean
    public ThreadPoolTaskExecutor ioTaskExecutor(
            @Value("${app.sync.thread-pool-size:4}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(200);  // reduced from 500; free tier won't queue more than this
        executor.setThreadNamePrefix("io-task-");
        executor.initialize();
        return executor;
    }
}