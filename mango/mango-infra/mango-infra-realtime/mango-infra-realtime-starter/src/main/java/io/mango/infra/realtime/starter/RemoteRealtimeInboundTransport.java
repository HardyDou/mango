package io.mango.infra.realtime.starter;

import io.mango.infra.realtime.api.RealtimeInboundMessage;
import io.mango.infra.realtime.api.RealtimeSubscriberRegistration;
import io.mango.infra.realtime.core.inbound.RealtimeInboundTransport;
import io.mango.infra.realtime.core.inbound.RealtimeSubscriberRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
public class RemoteRealtimeInboundTransport implements RealtimeInboundTransport {

    private final RealtimeSubscriberRegistry subscriberRegistry;
    private final RestTemplate restTemplate;

    @Override
    public void forward(RealtimeInboundMessage message) {
        for (RealtimeSubscriberRegistration registration : subscriberRegistry.findAll()) {
            try {
                restTemplate.postForEntity(receiverUrl(registration), message, Void.class);
            } catch (RestClientException e) {
                log.warn("Failed to forward realtime inbound message {} to service {}",
                        message.id(), registration.serviceName(), e);
            }
        }
    }

    private String receiverUrl(RealtimeSubscriberRegistration registration) {
        return "http://" + registration.serviceName()
                + normalizeContextPath(registration.contextPath())
                + registration.endpoint();
    }

    private String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }
}
