package io.mango.ai.starter;

import io.mango.ai.core.resource.AiProviderConnectionResourceHandler;
import io.mango.ai.core.service.impl.AiServiceChatService;
import io.mango.ai.starter.controller.AiServiceChatController;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.IRateLimiter;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

/**
 * Mango AI Spring AI 自动配置。
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
    "io.mango.infra.kv.starter.KvCapabilityAutoConfiguration",
    "io.mango.infra.persistence.starter.PersistenceAutoConfiguration",
    "io.mango.infra.realtime.starter.MangoRealtimeAutoConfiguration",
    "io.mango.infra.realtime.starter.remote.RealtimeRemoteAutoConfiguration"
})
@MapperScan(basePackages = "io.mango.ai.core.mapper", annotationClass = Mapper.class)
@ComponentScan(basePackageClasses = {
    AiProviderConnectionResourceHandler.class,
    AiServiceChatService.class,
    AiServiceChatController.class
})
public class MangoAiAutoConfiguration {

    /**
     * 校验 AI 对话所需的 Mango KV 能力。
     *
     * @param rateLimiter Mango 限流能力
     * @param environment Spring 配置环境
     * @return 启动期校验器
     */
    @Bean
    SmartInitializingSingleton mangoAiInfrastructureVerifier(
            IRateLimiter rateLimiter,
            Environment environment) {
        return () -> {
            String storeType = environment.getProperty("mango.kv.store.type");
            Require.isTrue(
                    "redis".equals(storeType) || "jdbc".equals(storeType),
                    "Mango AI requires mango.kv.store.type=redis or jdbc");
        };
    }
}
