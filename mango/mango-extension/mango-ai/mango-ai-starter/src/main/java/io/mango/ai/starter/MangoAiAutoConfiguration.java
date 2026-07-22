package io.mango.ai.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.core.controller.ChatController;
import io.mango.ai.core.provider.DeepSeekProvider;
import io.mango.ai.core.provider.IAiProvider;
import io.mango.ai.core.service.impl.AiPushService;
import io.mango.ai.core.service.impl.ChatService;
import io.netty.channel.ChannelOption;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * AI 扩展自动配置。
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
    "io.mango.infra.realtime.starter.MangoRealtimeAutoConfiguration",
    "io.mango.infra.realtime.starter.remote.RealtimeRemoteAutoConfiguration"
})
@EnableConfigurationProperties(MangoAiProperties.class)
@ComponentScan(basePackageClasses = {ChatController.class, ChatService.class, AiPushService.class})
public class MangoAiAutoConfiguration {

    private static final int MAX_IN_MEMORY_BYTES = 1024 * 1024;

    @Bean
    @ConditionalOnMissingBean
    IAiProvider aiProvider(ObjectMapper objectMapper, MangoAiProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeout())
                .responseTimeout(Duration.ofMillis(properties.readTimeout()));
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
                .build();
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
        return new DeepSeekProvider(
                objectMapper,
                webClient,
                properties.model(),
                Duration.ofMillis(properties.readTimeout()));
    }

}
