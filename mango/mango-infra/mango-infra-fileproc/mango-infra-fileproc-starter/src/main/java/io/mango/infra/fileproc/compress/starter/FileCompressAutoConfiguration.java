package io.mango.infra.fileproc.compress.starter;

import io.mango.infra.fileproc.compress.FileCompressApi;
import io.mango.infra.fileproc.compress.service.DefaultFileCompressApi;
import io.mango.infra.fileproc.compress.service.GhostscriptPdfFileCompressProvider;
import io.mango.infra.fileproc.compress.service.IFileCompressProvider;
import io.mango.infra.fileproc.compress.service.ImageFileCompressProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.List;

/**
 * 文件压缩能力自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(FileCompressProperties.class)
@ConditionalOnProperty(prefix = "mango.fileproc.compress", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileCompressAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ImageFileCompressProvider imageFileCompressProvider() {
        return new ImageFileCompressProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public GhostscriptPdfFileCompressProvider ghostscriptPdfFileCompressProvider(FileCompressProperties properties) {
        return new GhostscriptPdfFileCompressProvider(properties.getGhostscriptPath(),
                Duration.ofSeconds(properties.getTimeoutSeconds()));
    }

    @Bean
    @ConditionalOnMissingBean
    public FileCompressApi fileCompressApi(List<IFileCompressProvider> providers) {
        return new DefaultFileCompressApi(providers);
    }
}
