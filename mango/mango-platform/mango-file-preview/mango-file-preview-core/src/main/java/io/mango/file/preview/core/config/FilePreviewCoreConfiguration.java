package io.mango.file.preview.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 文件预览核心配置。
 */
@Configuration
public class FilePreviewCoreConfiguration {

    @Bean
    public Clock filePreviewClock() {
        return Clock.systemUTC();
    }
}
