package io.mango.file.starter;

import io.mango.file.core.config.FileProperties;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.storage.AliyunOssFileStorage;
import io.mango.file.core.storage.FileStorage;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.file.core.storage.LocalFileStorage;
import io.mango.file.core.storage.QiniuKodoFileStorage;
import io.mango.file.core.storage.S3CompatibleFileStorage;
import io.mango.file.core.storage.TencentCosFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * 文件能力自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(FileRecordMapper.class)
@ConditionalOnProperty(prefix = "mango.file", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FileProperties.class)
@MapperScan("io.mango.file.core.mapper")
@ComponentScan({
        "io.mango.file.core.service",
        "io.mango.file.starter"
    })
public class FileAutoConfiguration {

    @Bean
    public FileStorage localFileStorage(FileProperties properties) {
        return new LocalFileStorage(properties);
    }

    @Bean
    public FileStorage s3CompatibleFileStorage() {
        return new S3CompatibleFileStorage();
    }

    @Bean
    public FileStorage aliyunOssFileStorage() {
        return new AliyunOssFileStorage();
    }

    @Bean
    public FileStorage tencentCosFileStorage() {
        return new TencentCosFileStorage();
    }

    @Bean
    public FileStorage qiniuKodoFileStorage() {
        return new QiniuKodoFileStorage();
    }

    @Bean
    public FileStorageRouter fileStorageRouter(List<FileStorage> storages) {
        return new FileStorageRouter(storages);
    }

    @Bean
    public FilterRegistrationBean<LocalFileObjectFrameOptionsFilter> localFileObjectFrameOptionsFilter() {
        FilterRegistrationBean<LocalFileObjectFrameOptionsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LocalFileObjectFrameOptionsFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
