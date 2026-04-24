package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;
import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@Slf4j
@RequiredArgsConstructor
public class RealtimeInboundReceiverAutoRegistrar {

    private final RealtimeInboundReceiverFeignClient realtimeInboundReceiverApi;
    private final IRealtimeInboundService realtimeInboundService;
    private final RealtimeRemoteProperties properties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        if (!shouldRegister()) {
            return;
        }
        RealtimeInboundReceiverRegistration registration = registration();
        realtimeInboundReceiverApi.register(registration);
        log.info("Registered realtime inbound receiver service: {}", registration.serviceName());
    }

    @EventListener(ContextClosedEvent.class)
    public void unregister() {
        if (!shouldRegister()) {
            return;
        }
        realtimeInboundReceiverApi.unregister(registration());
    }

    private boolean shouldRegister() {
        return properties.getInbound().isEnabled()
                && properties.getInbound().getRemote().isRegisterEnabled()
                && realtimeInboundService.hasListeners();
    }

    private RealtimeInboundReceiverRegistration registration() {
        RealtimeRemoteProperties.Remote remote = properties.getInbound().getRemote();
        return new RealtimeInboundReceiverRegistration(
                serviceName(remote),
                contextPath(remote),
                remote.getEndpoint());
    }

    private String serviceName(RealtimeRemoteProperties.Remote remote) {
        if (remote.getServiceName() != null && !remote.getServiceName().isBlank()) {
            return remote.getServiceName();
        }
        return environment.getProperty("spring.application.name", "application");
    }

    private String contextPath(RealtimeRemoteProperties.Remote remote) {
        String configured = remote.getContextPath();
        if (configured != null && !configured.isBlank() && !"/".equals(configured)) {
            return configured;
        }
        return environment.getProperty("server.servlet.context-path", "/");
    }
}
