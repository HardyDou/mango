package io.mango.resource.starter.remote;

import io.mango.infra.feign.starter.ModuleTargetResolver;
import io.mango.resource.api.ResourceTargetDispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 资源注册中心远程自动配置。
 */
@Configuration
@EnableFeignClients(basePackages = "io.mango.resource.starter.remote")
public class ResourceRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResourceTargetDispatcher resourceTargetDispatcher(ModuleTargetResolver moduleTargetResolver,
                                                             ResourceTargetFeignClient targetFeignClient) {
        return new RemoteResourceTargetDispatcher(moduleTargetResolver, targetFeignClient);
    }
}
