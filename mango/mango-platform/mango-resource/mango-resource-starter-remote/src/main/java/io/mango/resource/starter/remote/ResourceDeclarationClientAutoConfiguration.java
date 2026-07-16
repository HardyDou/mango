package io.mango.resource.starter.remote;

import io.mango.resource.api.ResourceDeclarationApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * 资源声明注册客户端 Feign 装配。
 */
@Configuration
@ConditionalOnMissingBean(ResourceDeclarationApi.class)
@EnableFeignClients(clients = ResourceDeclarationFeignClient.class)
public class ResourceDeclarationClientAutoConfiguration {
}
