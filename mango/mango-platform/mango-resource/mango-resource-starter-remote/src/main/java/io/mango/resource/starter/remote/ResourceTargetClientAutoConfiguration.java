package io.mango.resource.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * 资源目标反向执行客户端 Feign 装配。
 */
@Configuration
@EnableFeignClients(clients = ResourceTargetFeignClient.class)
public class ResourceTargetClientAutoConfiguration {
}
