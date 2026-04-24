package io.mango.infra.realtime.starter.forward;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.IRealtimeOutboundForwardService;
import io.mango.infra.realtime.core.presence.RealtimePresence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class HttpRealtimeOutboundForwardService implements IRealtimeOutboundForwardService {

    private final RestOperations restOperations;

    @Override
    public void forward(Collection<RealtimePresence> presences, RealtimeOutboundMessage message) {
        for (RealtimePresence presence : uniqueRoutes(presences).values()) {
            try {
                restOperations.postForEntity(receiverUrl(presence), message, Void.class);
            } catch (RestClientException e) {
                log.warn("Failed to forward realtime outbound message {} to service {}",
                        message.id(), presence.serviceName(), e);
            }
        }
    }

    private Map<String, RealtimePresence> uniqueRoutes(Collection<RealtimePresence> presences) {
        Map<String, RealtimePresence> routes = new LinkedHashMap<>();
        if (presences == null) {
            return routes;
        }
        for (RealtimePresence presence : presences) {
            if (presence != null) {
                routes.putIfAbsent(presence.routeKey(), presence);
            }
        }
        return routes;
    }

    private String receiverUrl(RealtimePresence presence) {
        return "http://" + presence.serviceName()
                + presence.contextPath()
                + presence.outboundEndpoint();
    }
}
