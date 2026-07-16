package io.mango.file.starter.remote;

import io.mango.file.api.IFileContentProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * 文件服务远程调用自动配置。
 */
@AutoConfiguration
@EnableFeignClients(basePackageClasses = FileFeignClient.class)
public class FileRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IFileContentProvider.class)
    IFileContentProvider fileContentProvider(FileBinaryFeignClient fileBinaryFeignClient) {
        return new FileRemoteContentProvider(fileBinaryFeignClient);
    }
}
