package io.mango.file.preview.starter.remote;

import cn.keking.spi.FileDownloader;
import io.mango.file.preview.support.remote.FilePreviewRemoteSourceDownloader;
import io.mango.file.preview.support.remote.FilePreviewSourceTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

/**
 * 独立部署预览引擎的远程取源自动配置。
 *
 * <p>默认关闭：只有显式设置 {@code mango.file-preview.remote.enabled=true} 且配置了
 * {@code mango.file-preview.remote.base-url} 时，才会把 {@code fileId} 取源改走远程直取。</p>
 */
@AutoConfiguration(afterName = "io.mango.file.preview.starter.FilePreviewAutoConfiguration")
@ConditionalOnProperty(prefix = "mango.file-preview.remote", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FilePreviewRemoteProperties.class)
@EnableFeignClients(basePackageClasses = FilePreviewSourceFeignClient.class)
public class FilePreviewRemoteAutoConfiguration {

    /**
     * 远程取源传输：按票据流式取流。
     *
     * @param sourceFeignClient 源文件二进制客户端
     * @param properties        远程取源配置
     * @return 远程取源传输
     */
    @Bean
    @ConditionalOnMissingBean(FilePreviewSourceTransport.class)
    public FilePreviewSourceTransport filePreviewRemoteSourceTransport(
            FilePreviewSourceFeignClient sourceFeignClient,
            FilePreviewRemoteProperties properties) {
        Assert.hasText(properties.getBaseUrl(),
                "启用 mango.file-preview.remote.enabled 时必须配置 mango.file-preview.remote.base-url；"
                        + "若希望走 url 兜底取源，请关闭远程直取并配置 mango.file-preview.source-base-url");
        return new FilePreviewRemoteSourceTransport(sourceFeignClient);
    }

    /**
     * 引擎侧取源下载器：注册到引擎的 {@code FileDownloaders} 注册表。
     *
     * <p>内嵌部署已经装配了同进程直取实现，因此这里只在其缺失时注册。</p>
     *
     * @param transport 远程取源传输
     * @return 引擎取源下载器
     */
    @Bean
    @ConditionalOnMissingBean(FileDownloader.class)
    public FilePreviewRemoteSourceDownloader filePreviewRemoteSourceDownloader(
            FilePreviewSourceTransport transport) {
        return new FilePreviewRemoteSourceDownloader(transport);
    }
}
