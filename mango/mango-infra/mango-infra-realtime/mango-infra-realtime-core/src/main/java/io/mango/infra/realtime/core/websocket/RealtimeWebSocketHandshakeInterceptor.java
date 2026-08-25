package io.mango.infra.realtime.core.websocket;

import io.mango.infra.realtime.api.dto.RealtimeHeaders;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicket;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicketService;
import io.mango.infra.realtime.core.web.RealtimeRequestIdentityResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
public class RealtimeWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String TENANT_ID_ATTR = "tenantId";
    public static final String AUTHORIZED_ATTR = "authorized";
    public static final String USER_ID_ATTR = "userId";
    public static final String CLIENT_ID_ATTR = "clientId";
    public static final String PROFILE_ATTR = "profile";

    private final RealtimeConnectionTicketService ticketService;

    public RealtimeWebSocketHandshakeInterceptor(RealtimeConnectionTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WebSocket handshake rejected: non-servlet request attempted");
            return false;
        }

        String ticketValue = servletRequest.getServletRequest().getParameter("rtTicket");
        if (firstText(ticketValue) != null) {
            return ticketService.resolve(ticketValue)
                    .map(ticket -> copyTicketIdentity(ticket, attributes))
                    .orElseGet(() -> rejectExpiredTicket(response));
        }

        String tenantId = RealtimeRequestIdentityResolver.resolveTenantId(
                servletRequest.getServletRequest(),
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.TENANT_ID),
                servletRequest.getServletRequest().getParameter("tenantId"));
        Long userId = RealtimeRequestIdentityResolver.resolveUserId(
                servletRequest.getServletRequest(),
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.USER_ID),
                servletRequest.getServletRequest().getParameter("userId"));
        String clientId = firstText(
                servletRequest.getServletRequest().getHeader(RealtimeHeaders.CLIENT_ID),
                servletRequest.getServletRequest().getParameter("clientId"));

        attributes.put(TENANT_ID_ATTR, defaultTenantId(tenantId));
        attributes.put(AUTHORIZED_ATTR, true);
        attributes.put(PROFILE_ATTR, Map.of());
        if (userId != null) {
            attributes.put(USER_ID_ATTR, userId);
        }
        if (clientId != null) {
            attributes.put(CLIENT_ID_ATTR, clientId);
        }
        return true;
    }

    private boolean copyTicketIdentity(RealtimeConnectionTicket ticket, Map<String, Object> attributes) {
        attributes.put(TENANT_ID_ATTR, ticket.tenantId());
        attributes.put(AUTHORIZED_ATTR, true);
        attributes.put(PROFILE_ATTR, ticket.profile());
        if (ticket.userId() != null) {
            attributes.put(USER_ID_ATTR, ticket.userId());
        }
        if (firstText(ticket.clientId()) != null) {
            attributes.put(CLIENT_ID_ATTR, ticket.clientId());
        }
        return true;
    }

    private boolean rejectExpiredTicket(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String defaultTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "default";
        }
        return tenantId;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake.
    }
}
