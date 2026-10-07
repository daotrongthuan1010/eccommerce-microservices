package com.dtthuan3.ecommerce.fileservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class FileUploadExecutorConfig {

    @Bean("fileUploadExecutor")
    public Executor fileUploadExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Luôn duy trì 2 thread
        executor.setCorePoolSize(2);

        // Tối đa 6 thread chạy song song
        executor.setMaxPoolSize(6);

        // Queue tối đa 100 task
        executor.setQueueCapacity(100);

        // Thread dư sống tối đa 30 giây
        executor.setKeepAliveSeconds(30);

        executor.setThreadNamePrefix("file-upload-");

        /*
         * Nếu pool + queue đều đầy,
         * chính thread hiện tại sẽ xử lý task.
         *
         * Tránh việc vứt bỏ file.
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        return executor;
    }
}