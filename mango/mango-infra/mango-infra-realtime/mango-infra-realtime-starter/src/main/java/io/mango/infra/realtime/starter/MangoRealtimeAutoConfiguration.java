package io.mango.infra.realtime.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.realtime.api.RealtimePublisher;
import io.mango.infra.realtime.api.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.dispatcher.CompositeRealtimePublisher;
import io.mango.infra.realtime.core.dispatcher.ProtocolRealtimeSender;
import io.mango.infra.realtime.core.inbound.InMemoryRealtimeSubscriberRegistry;
import io.mango.infra.realtime.core.inbound.InboundUnknownTypePolicy;
import io.mango.infra.realtime.core.inbound.LocalRealtimeInboundDispatcher;
import io.mango.infra.realtime.core.inbound.LocalRealtimeInboundTransport;
import io.mango.infra.realtime.core.inbound.NoopRealtimeInboundTransport;
import io.mango.infra.realtime.core.inbound.RealtimeInboundDispatcher;
import io.mango.infra.realtime.core.inbound.RealtimeInboundTransport;
import io.mango.infra.realtime.core.inbound.RealtimeSubscriberRegistry;
import io.mango.infra.realtime.core.negotiate.RealtimeNegotiationController;
import io.mango.infra.realtime.core.negotiate.RealtimeTransportCapability;
import io.mango.infra.realtime.core.polling.InMemoryRealtimePollingService;
import io.mango.infra.realtime.core.polling.PollingProtocolAdapter;
import io.mango.infra.realtime.core.polling.PollingRealtimeController;
import io.mango.infra.realtime.core.session.InMemoryRealtimeSubscriptionManager;
import io.mango.infra.realtime.core.sse.SseProtocolAdapter;
import io.mango.infra.realtime.core.sse.SseRealtimeController;
import io.mango.infra.realtime.core.websocket.RealtimeWebSocketConfiguration;
import io.mango.infra.realtime.core.websocket.RealtimeWebSocketHandler;
import io.mango.infra.realtime.core.websocket.RealtimeWebSocketHandshakeInterceptor;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSocket
@EnableConfigurationProperties(MangoRealtimeProperties.class)
@Conditional(RealtimeConditions.Enabled.class)
public class MangoRealtimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Conditional(RealtimeConditions.ConnectionProtocolEnabled.class)
    public RealtimeSubscriptionManager subscriptionManager() {
        return new InMemoryRealtimeSubscriptionManager();
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(RealtimeConditions.PollingEnabled.class)
    public InMemoryRealtimePollingService realtimePollingService(MangoRealtimeProperties properties) {
        return new InMemoryRealtimePollingService(properties.getPolling().getDefaultMaxSize());
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(RealtimeConditions.PublishEnabled.class)
    public RealtimePublisher realtimePublisher(List<ProtocolRealtimeSender> senders) {
        return new CompositeRealtimePublisher(senders);
    }

    @Bean
    @ConditionalOnBean(RealtimePublisher.class)
    @Conditional(RealtimeConditions.RemoteEndpointEnabled.class)
    public RealtimeApiController realtimeApiController(RealtimePublisher realtimePublisher) {
        return new RealtimeApiController(realtimePublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public RealtimeInboundDispatcher realtimeInboundDispatcher(ListableBeanFactory beanFactory,
                                                               MangoRealtimeProperties properties) {
        MangoRealtimeProperties.Inbound inbound = properties.getInbound();
        return new LocalRealtimeInboundDispatcher(
                beanFactory,
                inbound.isFailFast(),
                unknownTypePolicy(inbound.getUnknownTypePolicy()));
    }

    @Bean
    @ConditionalOnMissingBean
    public RealtimeSubscriberRegistry realtimeSubscriberRegistry() {
        return new InMemoryRealtimeSubscriberRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public RealtimeInboundTransport realtimeInboundTransport(MangoRealtimeProperties properties,
                                                             RealtimeInboundDispatcher dispatcher,
                                                             RealtimeSubscriberRegistry subscriberRegistry) {
        MangoRealtimeProperties.Inbound inbound = properties.getInbound();
        if (!inbound.isEnabled() || inbound.getMode() == RealtimeInboundMode.NONE) {
            return new NoopRealtimeInboundTransport();
        }
        if (inbound.getMode() == RealtimeInboundMode.LOCAL) {
            return new LocalRealtimeInboundTransport(dispatcher);
        }
        if (inbound.getMode() == RealtimeInboundMode.REMOTE) {
            return new RemoteRealtimeInboundTransport(subscriberRegistry, new RestTemplate());
        }
        return new NoopRealtimeInboundTransport();
    }

    @Bean
    @ConditionalOnBean(RealtimeSubscriberRegistry.class)
    public RealtimeSubscriberApiController realtimeSubscriberApiController(RealtimeSubscriberRegistry registry) {
        return new RealtimeSubscriberApiController(registry);
    }

    @Bean
    @Conditional(RealtimeConditions.NegotiateEnabled.class)
    public RealtimeNegotiationController realtimeNegotiationController(MangoRealtimeProperties properties) {
        return new RealtimeNegotiationController(transportCapabilities(properties));
    }

    @Bean
    @ConditionalOnBean(InMemoryRealtimePollingService.class)
    public PollingProtocolAdapter pollingProtocolAdapter(InMemoryRealtimePollingService pollingService) {
        return new PollingProtocolAdapter(pollingService);
    }

    @Bean
    @ConditionalOnBean(InMemoryRealtimePollingService.class)
    public PollingRealtimeController pollingRealtimeController(InMemoryRealtimePollingService pollingService,
                                                               MangoRealtimeProperties properties) {
        MangoRealtimeProperties.Polling polling = properties.getPolling();
        return new PollingRealtimeController(
                pollingService,
                polling.getDefaultMaxSize(),
                polling.getMaxSize(),
                polling.getDefaultTimeoutMillis(),
                polling.getMaxTimeoutMillis());
    }

    @Bean
    @ConditionalOnClass(SseEmitter.class)
    @Conditional(RealtimeConditions.SseEnabled.class)
    public SseProtocolAdapter sseProtocolAdapter(RealtimeSubscriptionManager subscriptionManager,
                                                 MangoRealtimeProperties properties) {
        return new SseProtocolAdapter(subscriptionManager, properties.getSse().getTimeoutMillis());
    }

    @Bean
    @ConditionalOnBean(SseProtocolAdapter.class)
    public SseRealtimeController sseRealtimeController(SseProtocolAdapter sseProtocolAdapter) {
        return new SseRealtimeController(sseProtocolAdapter);
    }

    @Bean
    @ConditionalOnClass(WebSocketConfigurer.class)
    @Conditional(RealtimeConditions.WebSocketEnabled.class)
    public RealtimeWebSocketHandshakeInterceptor realtimeWebSocketHandshakeInterceptor() {
        return new RealtimeWebSocketHandshakeInterceptor();
    }

    @Bean
    @ConditionalOnBean(RealtimeWebSocketHandshakeInterceptor.class)
    public RealtimeWebSocketHandler realtimeWebSocketHandler(RealtimeSubscriptionManager subscriptionManager,
                                                             ObjectMapper objectMapper,
                                                             RealtimeInboundTransport inboundTransport,
                                                             MangoRealtimeProperties properties) {
        return new RealtimeWebSocketHandler(
                subscriptionManager,
                objectMapper,
                inboundTransport,
                properties.getInbound().getMaxPayloadBytes());
    }

    @Bean
    @ConditionalOnBean({RealtimeWebSocketHandler.class, RealtimeWebSocketHandshakeInterceptor.class})
    public RealtimeWebSocketConfiguration realtimeWebSocketConfiguration(
            RealtimeWebSocketHandler webSocketHandler,
            RealtimeWebSocketHandshakeInterceptor handshakeInterceptor,
            MangoRealtimeProperties properties) {
        return new RealtimeWebSocketConfiguration(
                webSocketHandler,
                handshakeInterceptor,
                properties.getWebsocket().getEndpoint(),
                properties.getWebsocket().getAllowedOrigins());
    }

    private List<RealtimeTransportCapability> transportCapabilities(MangoRealtimeProperties properties) {
        List<RealtimeTransportCapability> transports = new ArrayList<>();
        if (properties.isWebsocketEffectiveEnabled()) {
            transports.add(new RealtimeTransportCapability(
                    "websocket",
                    true,
                    properties.getWebsocket().getEndpoint(),
                    true,
                    false,
                    null,
                    null,
                    null,
                    null));
        }
        if (properties.isSseEffectiveEnabled()) {
            transports.add(new RealtimeTransportCapability(
                    "sse",
                    true,
                    properties.getSse().getEndpoint(),
                    false,
                    false,
                    null,
                    null,
                    null,
                    null));
        }
        if (properties.isPollingEffectiveEnabled()) {
            MangoRealtimeProperties.Polling polling = properties.getPolling();
            transports.add(new RealtimeTransportCapability(
                    "polling",
                    true,
                    polling.getEndpoint(),
                    false,
                    true,
                    polling.getDefaultMaxSize(),
                    polling.getMaxSize(),
                    polling.getDefaultTimeoutMillis(),
                    polling.getMaxTimeoutMillis()));
        }
        return List.copyOf(transports);
    }

    private InboundUnknownTypePolicy unknownTypePolicy(String value) {
        return InboundUnknownTypePolicy.valueOf(value.trim().toUpperCase());
    }
}
