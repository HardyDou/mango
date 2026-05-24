package io.mango.file.starter.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 文件服务远程调用自动配置。
 */
@AutoConfiguration
@EnableFeignClients(basePackageClasses = FileFeignClient.class)
public class FileRemoteAutoConfiguration {
}
