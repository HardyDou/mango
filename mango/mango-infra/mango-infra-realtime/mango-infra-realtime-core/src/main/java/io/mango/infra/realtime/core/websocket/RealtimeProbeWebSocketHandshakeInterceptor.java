package io.mango.infra.realtime.core.websocket;

import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicket;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicketService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Requires an issued short-lived ticket before the unauthenticated WebSocket probe is opened.
 */
public class RealtimeProbeWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final RealtimeConnectionTicketService ticketService;

    public RealtimeProbeWebSocketHandshakeInterceptor(RealtimeConnectionTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String ticketValue = servletRequest.getServletRequest().getParameter("rtTicket");
        return ticketService.resolve(ticketValue)
                .map(ticket -> copyIdentity(ticket, attributes))
                .orElse(false);
    }

    private boolean copyIdentity(RealtimeConnectionTicket ticket, Map<String, Object> attributes) {
        attributes.put(RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, ticket.tenantId());
        attributes.put(RealtimeWebSocketHandshakeInterceptor.AUTHORIZED_ATTR, true);
        attributes.put(RealtimeWebSocketHandshakeInterceptor.PROFILE_ATTR, ticket.profile());
        if (ticket.userId() != null) {
            attributes.put(RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR, ticket.userId());
        }
        if (ticket.clientId() != null && !ticket.clientId().isBlank()) {
            attributes.put(RealtimeWebSocketHandshakeInterceptor.CLIENT_ID_ATTR, ticket.clientId());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // No state is allocated by this interceptor after a successful handshake.
    }
}
