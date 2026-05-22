package io.mango.template.starter.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 模板服务远程调用自动配置。
 */
@AutoConfiguration
@EnableFeignClients(basePackageClasses = TemplateFeignClient.class)
public class TemplateRemoteAutoConfiguration {
}
