package org.example.boykisserai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // Включает поддержку асинхронных методов @Async в Spring
public class AsyncConfig {

    @Bean(name = "vtuberAsyncExecutor")
    public Executor vtuberAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Сколько потоков всегда будут активны
        executor.setCorePoolSize(4);
        // Максимальное количество потоков при высокой нагрузке
        executor.setMaxPoolSize(10);
        // Размер очереди для задач, если все потоки заняты
        executor.setQueueCapacity(50);
        // Префикс для имен потоков в логах
        executor.setThreadNamePrefix("VTuberAsync-");
        executor.initialize();
        return executor;
    }
}