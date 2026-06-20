package io.mango.resource.starter.remote;

import io.mango.resource.api.ResourceRegistryApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * 资源注册中心客户端 Feign 装配。
 */
@Configuration
@ConditionalOnMissingBean(ResourceRegistryApi.class)
@EnableFeignClients(clients = ResourceRegistryFeignClient.class)
public class ResourceRegistryClientAutoConfiguration {
}
