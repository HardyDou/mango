package io.mango.infra.realtime.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.realtime.api.RealtimePollingService;
import io.mango.infra.realtime.api.RealtimePublisher;
import io.mango.infra.realtime.api.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.negotiate.RealtimeNegotiationController;
import io.mango.infra.realtime.core.polling.PollingRealtimeController;
import io.mango.infra.realtime.core.sse.SseProtocolAdapter;
import io.mango.infra.realtime.core.websocket.RealtimeWebSocketConfiguration;
import io.mango.infra.realtime.core.websocket.RealtimeWebSocketHandler;
import io.mango.infra.realtime.core.websocket.RealtimeWebSocketHandshakeInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MangoRealtimeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MangoRealtimeAutoConfiguration.class));

    @Test
    void enabledByDefault_createsCoreAndProtocolBeans() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(RealtimeSubscriptionManager.class);
            assertThat(ctx).hasSingleBean(RealtimePollingService.class);
            assertThat(ctx).hasSingleBean(RealtimePublisher.class);
            assertThat(ctx).hasSingleBean(RealtimeApiController.class);
            assertThat(ctx).hasSingleBean(RealtimeNegotiationController.class);
            assertThat(ctx).hasSingleBean(PollingRealtimeController.class);
            assertThat(ctx).hasSingleBean(SseProtocolAdapter.class);
            assertThat(ctx).hasSingleBean(RealtimeWebSocketHandshakeInterceptor.class);
            assertThat(ctx).hasSingleBean(RealtimeWebSocketHandler.class);
            assertThat(ctx).hasSingleBean(RealtimeWebSocketConfiguration.class);
        });
    }

    @Test
    void disabled_masterSwitch_createsNoRealtimeBeans() {
        contextRunner
                .withPropertyValues("mango.infra.realtime.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(RealtimeSubscriptionManager.class);
                    assertThat(ctx).doesNotHaveBean(RealtimePollingService.class);
                    assertThat(ctx).doesNotHaveBean(RealtimePublisher.class);
                    assertThat(ctx).doesNotHaveBean(RealtimeApiController.class);
                    assertThat(ctx).doesNotHaveBean(RealtimeNegotiationController.class);
                    assertThat(ctx).doesNotHaveBean(SseProtocolAdapter.class);
                    assertThat(ctx).doesNotHaveBean(RealtimeWebSocketHandler.class);
                });
    }

    @Test
    void modeSse_createsOnlySsePublishingProtocol() {
        contextRunner
                .withPropertyValues("mango.infra.realtime.mode=sse")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RealtimeSubscriptionManager.class);
                    assertThat(ctx).hasSingleBean(RealtimePublisher.class);
                    assertThat(ctx).hasSingleBean(RealtimeApiController.class);
                    assertThat(ctx).hasSingleBean(RealtimeNegotiationController.class);
                    assertThat(ctx).hasSingleBean(SseProtocolAdapter.class);
                    assertThat(ctx).doesNotHaveBean(RealtimeWebSocketHandler.class);
                    assertThat(ctx).doesNotHaveBean(RealtimePollingService.class);
                });
    }

    @Test
    void modeWebsocket_createsOnlyWebSocketPublishingProtocol() {
        contextRunner
                .withPropertyValues("mango.infra.realtime.mode=websocket")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RealtimeSubscriptionManager.class);
                    assertThat(ctx).hasSingleBean(RealtimePublisher.class);
                    assertThat(ctx).hasSingleBean(RealtimeApiController.class);
                    assertThat(ctx).hasSingleBean(RealtimeNegotiationController.class);
                    assertThat(ctx).doesNotHaveBean(SseProtocolAdapter.class);
                    assertThat(ctx).hasSingleBean(RealtimeWebSocketHandler.class);
                    assertThat(ctx).doesNotHaveBean(RealtimePollingService.class);
                });
    }

    @Test
    void modePolling_createsOnlyPollingProtocol() {
        contextRunner
                .withPropertyValues("mango.infra.realtime.mode=polling")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(RealtimeSubscriptionManager.class);
                    assertThat(ctx).hasSingleBean(RealtimePublisher.class);
                    assertThat(ctx).hasSingleBean(RealtimeApiController.class);
                    assertThat(ctx).hasSingleBean(RealtimeNegotiationController.class);
                    assertThat(ctx).hasSingleBean(PollingRealtimeController.class);
                    assertThat(ctx).doesNotHaveBean(SseProtocolAdapter.class);
                    assertThat(ctx).doesNotHaveBean(RealtimeWebSocketHandler.class);
                    assertThat(ctx).hasSingleBean(RealtimePollingService.class);
                });
    }

    @Test
    void autoMode_respectsNestedProtocolSwitches() {
        contextRunner
                .withPropertyValues(
                        "mango.infra.realtime.sse.enabled=false",
                        "mango.infra.realtime.websocket.enabled=false"
                )
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(RealtimeSubscriptionManager.class);
                    assertThat(ctx).hasSingleBean(RealtimePublisher.class);
                    assertThat(ctx).hasSingleBean(RealtimeApiController.class);
                    assertThat(ctx).hasSingleBean(RealtimeNegotiationController.class);
                    assertThat(ctx).doesNotHaveBean(SseProtocolAdapter.class);
                    assertThat(ctx).doesNotHaveBean(RealtimeWebSocketHandler.class);
                    assertThat(ctx).hasSingleBean(RealtimePollingService.class);
                });
    }

    @Test
    void disabledRemoteEndpoint_doesNotCreateController() {
        contextRunner
                .withPropertyValues("mango.infra.realtime.remote.endpoint-enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RealtimePublisher.class);
                    assertThat(ctx).doesNotHaveBean(RealtimeApiController.class);
                });
    }

    @Test
    void disabledNegotiation_doesNotCreateNegotiationController() {
        contextRunner
                .withPropertyValues("mango.infra.realtime.negotiate.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RealtimePublisher.class);
                    assertThat(ctx).doesNotHaveBean(RealtimeNegotiationController.class);
                });
    }

    @Test
    void customObjectMapper_preservesUserBean() {
        ObjectMapper objectMapper = new ObjectMapper();

        contextRunner
                .withBean(ObjectMapper.class, () -> objectMapper)
                .run(ctx -> assertThat(ctx.getBean(ObjectMapper.class)).isSameAs(objectMapper));
    }

    @Test
    void properties_customValues_bindRealtimeSettings() {
        contextRunner
                .withPropertyValues(
                        "mango.infra.realtime.mode=auto",
                        "mango.infra.realtime.sse.timeout-millis=1000",
                        "mango.infra.realtime.sse.endpoint=/stream/events",
                        "mango.infra.realtime.websocket.endpoint=/stream/ws",
                        "mango.infra.realtime.websocket.allowed-origins[0]=https://app.example.com",
                        "mango.infra.realtime.polling.endpoint=/stream/poll",
                        "mango.infra.realtime.polling.default-max-size=5",
                        "mango.infra.realtime.polling.max-size=50",
                        "mango.infra.realtime.polling.default-timeout-millis=1000",
                        "mango.infra.realtime.polling.max-timeout-millis=20000",
                        "mango.infra.realtime.negotiate.endpoint=/stream/negotiate")
                .run(ctx -> {
                    MangoRealtimeProperties properties = ctx.getBean(MangoRealtimeProperties.class);
                    assertThat(properties.getMode()).isEqualTo(RealtimeMode.AUTO);
                    assertThat(properties.getSse().getTimeoutMillis()).isEqualTo(1000);
                    assertThat(properties.getSse().getEndpoint()).isEqualTo("/stream/events");
                    assertThat(properties.getWebsocket().getEndpoint()).isEqualTo("/stream/ws");
                    assertThat(properties.getWebsocket().getAllowedOrigins()).containsExactly("https://app.example.com");
                    assertThat(properties.getPolling().getEndpoint()).isEqualTo("/stream/poll");
                    assertThat(properties.getPolling().getDefaultMaxSize()).isEqualTo(5);
                    assertThat(properties.getPolling().getMaxSize()).isEqualTo(50);
                    assertThat(properties.getPolling().getDefaultTimeoutMillis()).isEqualTo(1000);
                    assertThat(properties.getPolling().getMaxTimeoutMillis()).isEqualTo(20000);
                    assertThat(properties.getNegotiate().getEndpoint()).isEqualTo("/stream/negotiate");
                });
    }
}
