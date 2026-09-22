package io.mango.file.preview.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件预览核心配置。
 */
@Configuration
public class FilePreviewCoreConfiguration {

    @Bean
    public Clock filePreviewClock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService filePreviewConversionExecutor(FilePreviewProperties properties) {
        int workerCount = properties.getConversion().effectiveWorkerCount();
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "file-preview-convert-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(workerCount, threadFactory);
    }
}
