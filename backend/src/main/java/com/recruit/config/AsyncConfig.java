package com.recruit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * 目前承载操作日志等旁路写库任务，避免占用请求线程
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 日志写入专用线程池
     * CallerRunsPolicy：队列满时回退调用线程执行，日志不丢也不至于堆积
     */
    @Bean("logExecutor")
    public ThreadPoolTaskExecutor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("log-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
