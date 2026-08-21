package io.mango.ai.starter;

import io.mango.ai.core.service.impl.AiPushService;
import io.mango.ai.core.service.impl.ChatService;
import io.mango.ai.starter.controller.ChatController;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.IRateLimiter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Mango AI Spring AI 自动配置。
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration",
        "io.mango.infra.kv.starter.KvCapabilityAutoConfiguration",
        "io.mango.infra.realtime.starter.MangoRealtimeAutoConfiguration",
        "io.mango.infra.realtime.starter.remote.RealtimeRemoteAutoConfiguration"
})
@ComponentScan(basePackageClasses = {ChatController.class, ChatService.class, AiPushService.class})
public class MangoAiAutoConfiguration {

    /**
     * 校验 AI 对话所需的外部模型凭据和 Redis-backed Mango KV 能力。
     *
     * @param chatModel Spring AI 模型
     * @param cache Mango 缓存能力
     * @param rateLimiter Mango 限流能力
     * @param environment Spring 配置环境
     * @return 启动期校验器
     */
    @Bean
    SmartInitializingSingleton mangoAiInfrastructureVerifier(
            ChatModel chatModel,
            ICache cache,
            IRateLimiter rateLimiter,
            Environment environment) {
        return () -> {
            Require.isTrue(
                    "redis".equals(environment.getProperty("mango.kv.store.type")),
                    "Mango AI requires mango.kv.store.type=redis");
            Require.notBlank(
                    environment.getProperty("spring.ai.deepseek.api-key"),
                    "Mango AI requires a Spring AI DeepSeek API key");
        };
    }
}
