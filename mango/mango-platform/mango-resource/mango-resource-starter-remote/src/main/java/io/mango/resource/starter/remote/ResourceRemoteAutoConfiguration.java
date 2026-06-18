package io.mango.resource.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * 资源注册中心远程自动配置。
 */
@Configuration
@EnableFeignClients(basePackages = "io.mango.resource.starter.remote")
public class ResourceRemoteAutoConfiguration {
}
