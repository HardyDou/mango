package io.mango.infra.realtime.core.websocket;

import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicket;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeProbeWebSocketHandshakeInterceptorTest {

    @Test
    void rejectsMissingOrUnknownTicket() {
        RealtimeProbeWebSocketHandshakeInterceptor interceptor =
                new RealtimeProbeWebSocketHandshakeInterceptor(new RealtimeConnectionTicketService());

        assertThat(handshake(interceptor, null, new HashMap<>())).isFalse();
        assertThat(handshake(interceptor, "unknown", new HashMap<>())).isFalse();
    }

    @Test
    void acceptsIssuedTicketAndCopiesTrustedIdentity() {
        RealtimeConnectionTicketService ticketService = new RealtimeConnectionTicketService();
        RealtimeConnectionTicket ticket = ticketService.issue("tenant-a", 1001L, "browser-a", Map.of("theme", "dark"));
        Map<String, Object> attributes = new HashMap<>();

        assertThat(handshake(ticketService, ticket.value(), attributes)).isTrue();
        assertThat(attributes)
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, "tenant-a")
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR, 1001L)
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.CLIENT_ID_ATTR, "browser-a");
    }

    private boolean handshake(RealtimeConnectionTicketService ticketService,
                              String ticket,
                              Map<String, Object> attributes) {
        return handshake(new RealtimeProbeWebSocketHandshakeInterceptor(ticketService), ticket, attributes);
    }

    private boolean handshake(RealtimeProbeWebSocketHandshakeInterceptor interceptor,
                              String ticket,
                              Map<String, Object> attributes) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (ticket != null) {
            request.setParameter("rtTicket", ticket);
        }
        return interceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()),
                new ProbeWebSocketHandler(),
                attributes);
    }
}
