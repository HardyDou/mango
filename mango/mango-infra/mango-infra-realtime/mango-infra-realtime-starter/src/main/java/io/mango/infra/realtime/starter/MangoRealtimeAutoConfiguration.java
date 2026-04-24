package io.mango.infra.realtime.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mango.infra.kv.api.IKvSortedSet;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.core.inbound.forward.IRealtimeInboundForwardService;
import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.inbound.forward.RealtimeInboundForwardServices;
import io.mango.infra.realtime.core.inbound.receiver.IRealtimeInboundReceiverService;
import io.mango.infra.realtime.core.inbound.receiver.InMemoryRealtimeInboundReceiverService;
import io.mango.infra.realtime.core.negotiate.RealtimeTransportCapability;
import io.mango.infra.realtime.core.outbound.IRealtimeOutboundForwardService;
import io.mango.infra.realtime.core.outbound.IRealtimePublishService;
import io.mango.infra.realtime.core.outbound.RealtimeProtocolSender;
import io.mango.infra.realtime.core.outbound.RealtimePublishService;
import io.mango.infra.realtime.core.polling.InMemoryRealtimePollingService;
import io.mango.infra.realtime.core.polling.PollingProtocolAdapter;
import io.mango.infra.realtime.core.presence.IRealtimePresenceService;
import io.mango.infra.realtime.core.presence.InMemoryRealtimePresenceService;
import io.mango.infra.realtime.core.presence.RealtimeNode;
import io.mango.infra.realtime.core.session.InMemoryRealtimeSubscriptionManager;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.sse.SseProtocolAdapter;
import io.mango.infra.realtime.starter.controller.PollingRealtimeController;
import io.mango.infra.realtime.starter.controller.RealtimeApiController;
import io.mango.infra.realtime.starter.controller.RealtimeInboundReceiverController;
import io.mango.infra.realtime.starter.controller.RealtimeNegotiationController;
import io.mango.infra.realtime.starter.controller.RealtimeOutboundController;
import io.mango.infra.realtime.starter.controller.SseRealtimeController;
import io.mango.infra.realtime.starter.forward.HttpRealtimeOutboundForwardService;
import io.mango.infra.realtime.starter.forward.RemoteRealtimeInboundForwardService;
import io.mango.infra.realtime.starter.presence.KvRealtimePresenceService;
import io.mango.infra.realtime.core.websocket.RealtimeWebSocketConfiguration;
import io.mango.infra.realtime.core.websocket.RealtimeWebSocketHandler;
import io.mango.infra.realtime.core.websocket.RealtimeWebSocketHandshakeInterceptor;
import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;
import io.mango.infra.realtime.support.inbound.RealtimeInboundService;
import io.mango.infra.realtime.support.inbound.RealtimeInboundUnknownTypePolicy;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestOperations;
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
    public RealtimeSubscriptionManager subscriptionManager(IRealtimePresenceService realtimePresenceService,
                                                           RealtimeNode realtimeNode) {
        return new InMemoryRealtimeSubscriptionManager(realtimePresenceService, realtimeNode);
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
    public IRealtimePublishService realtimePublishService(List<RealtimeProtocolSender> senders,
                                                          IRealtimePresenceService realtimePresenceService,
                                                          IRealtimeOutboundForwardService outboundForwardService,
                                                          RealtimeNode realtimeNode) {
        return new RealtimePublishService(senders, realtimePresenceService, outboundForwardService, realtimeNode);
    }

    @Bean
    @ConditionalOnBean(IRealtimePublishService.class)
    @Conditional(RealtimeConditions.RemoteEndpointEnabled.class)
    public RealtimeApi realtimeApi(IRealtimePublishService realtimePublishService) {
        return new RealtimeApiController(realtimePublishService);
    }

    @Bean
    @ConditionalOnBean(IRealtimePublishService.class)
    @Conditional(RealtimeConditions.OutboundEndpointEnabled.class)
    public RealtimeOutboundController realtimeOutboundController(IRealtimePublishService realtimePublishService) {
        return new RealtimeOutboundController(realtimePublishService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public IRealtimeInboundService realtimeInboundService(ListableBeanFactory beanFactory,
                                                          MangoRealtimeProperties properties) {
        MangoRealtimeProperties.Inbound inbound = properties.getInbound();
        return new RealtimeInboundService(
                beanFactory,
                inbound.isFailFast(),
                unknownTypePolicy(inbound.getUnknownTypePolicy()));
    }

    @Bean
    @ConditionalOnMissingBean
    public IRealtimeInboundReceiverService realtimeInboundReceiverService() {
        return new InMemoryRealtimeInboundReceiverService();
    }

    @Bean
    @ConditionalOnMissingBean
    public IRealtimeInboundForwardService realtimeInboundForwardService(MangoRealtimeProperties properties,
                                                                        IRealtimeInboundService realtimeInboundService,
                                                                        IRealtimeInboundReceiverService realtimeInboundReceiverService,
                                                                        RestOperations realtimeRestOperations) {
        MangoRealtimeProperties.Inbound inbound = properties.getInbound();
        if (!inbound.isEnabled() || inbound.getMode() == RealtimeInboundMode.NONE) {
            return RealtimeInboundForwardServices.noop();
        }
        if (inbound.getMode() == RealtimeInboundMode.LOCAL) {
            return RealtimeInboundForwardServices.local(realtimeInboundService);
        }
        if (inbound.getMode() == RealtimeInboundMode.REMOTE) {
            return new RemoteRealtimeInboundForwardService(realtimeInboundReceiverService, realtimeRestOperations);
        }
        if (inbound.getMode() == RealtimeInboundMode.LOCAL_REMOTE) {
            return RealtimeInboundForwardServices.composite(List.of(
                    RealtimeInboundForwardServices.local(realtimeInboundService),
                    new RemoteRealtimeInboundForwardService(realtimeInboundReceiverService, realtimeRestOperations)));
        }
        return RealtimeInboundForwardServices.noop();
    }

    @Bean
    @ConditionalOnMissingBean
    public IRealtimePresenceService realtimePresenceService(ObjectProvider<IKvStore> kvStoreProvider,
                                                            ObjectMapper objectMapper,
                                                            MangoRealtimeProperties properties) {
        IKvStore kvStore = kvStoreProvider.getIfAvailable();
        if (kvStore instanceof IKvSortedSet sortedSet) {
            MangoRealtimeProperties.Presence presence = properties.getPresence();
            return new KvRealtimePresenceService(
                    kvStore,
                    sortedSet,
                    objectMapper,
                    presence.getPrefix(),
                    presence.getTtlSeconds());
        }
        return new InMemoryRealtimePresenceService();
    }

    @Bean
    @ConditionalOnMissingBean
    public IRealtimeOutboundForwardService realtimeOutboundForwardService(RestOperations realtimeRestOperations) {
        return new HttpRealtimeOutboundForwardService(realtimeRestOperations);
    }

    @Bean
    @ConditionalOnMissingBean
    public RealtimeNode realtimeNode(MangoRealtimeProperties properties, Environment environment) {
        MangoRealtimeProperties.Node node = properties.getNode();
        return new RealtimeNode(
                firstText(node.getInstanceId(),
                        environment.getProperty("mango.infra.realtime.node.instance-id"),
                        environment.getProperty("spring.application.instance_id"),
                        environment.getProperty("spring.application.name"),
                        "application"),
                firstText(node.getServiceName(),
                        environment.getProperty("spring.application.name"),
                        "application"),
                firstText(node.getContextPath(),
                        environment.getProperty("server.servlet.context-path"),
                        "/"),
                properties.getOutbound().getEndpoint());
    }

    @Bean
    @ConditionalOnMissingBean
    public RestOperations realtimeRestOperations() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnBean(IRealtimeInboundReceiverService.class)
    public RealtimeInboundReceiverController realtimeInboundReceiverController(
            IRealtimeInboundReceiverService realtimeInboundReceiverService) {
        return new RealtimeInboundReceiverController(realtimeInboundReceiverService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProtocolRealtimeInboundForwarder protocolRealtimeInboundForwarder(IRealtimeInboundForwardService inboundForwardService) {
        return new ProtocolRealtimeInboundForwarder(inboundForwardService);
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
                                                               MangoRealtimeProperties properties,
                                                               ProtocolRealtimeInboundForwarder inboundForwarder) {
        MangoRealtimeProperties.Polling polling = properties.getPolling();
        return new PollingRealtimeController(
                pollingService,
                polling.getDefaultMaxSize(),
                polling.getMaxSize(),
                polling.getDefaultTimeoutMillis(),
                polling.getMaxTimeoutMillis(),
                inboundForwarder);
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
    public SseRealtimeController sseRealtimeController(SseProtocolAdapter sseProtocolAdapter,
                                                       ProtocolRealtimeInboundForwarder inboundForwarder) {
        return new SseRealtimeController(sseProtocolAdapter, inboundForwarder);
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
                                                             IRealtimeInboundForwardService inboundForwardService,
                                                             MangoRealtimeProperties properties) {
        return new RealtimeWebSocketHandler(
                subscriptionManager,
                objectMapper,
                inboundForwardService,
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

    private RealtimeInboundUnknownTypePolicy unknownTypePolicy(String value) {
        return RealtimeInboundUnknownTypePolicy.valueOf(value.trim().toUpperCase());
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
