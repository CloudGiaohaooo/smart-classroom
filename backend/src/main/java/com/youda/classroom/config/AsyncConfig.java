package com.youda.classroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行配置：供学习行为埋点等「旁路写」使用，使其不阻塞业务主流程。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 行为埋点专用线程池：核心 2 / 最大 4，有界队列 1000。
     * 满载时采用 CallerRunsPolicy —— 降级为调用线程同步执行，既不平白丢日志，也不无界堆积内存。
     */
    @Bean("behaviorExecutor")
    public Executor behaviorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("behavior-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
