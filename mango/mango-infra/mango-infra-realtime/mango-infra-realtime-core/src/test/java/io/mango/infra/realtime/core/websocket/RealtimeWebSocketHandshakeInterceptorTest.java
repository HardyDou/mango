package io.mango.infra.realtime.core.websocket;

import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicket;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeWebSocketHandshakeInterceptorTest {

    @Test
    void issuedTicketRestoresTrustedIdentityInsteadOfRequestIdentity() {
        RealtimeConnectionTicketService ticketService = new RealtimeConnectionTicketService();
        RealtimeConnectionTicket ticket = ticketService.issue(
                "trusted-tenant", 1001L, "trusted-client", Map.of("realm", "admin"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("rtTicket", ticket.value());
        request.setParameter("tenantId", "spoofed-tenant");
        request.setParameter("userId", "9999");
        request.setParameter("clientId", "spoofed-client");
        Map<String, Object> attributes = new HashMap<>();

        assertThat(handshake(ticketService, request, new MockHttpServletResponse(), attributes)).isTrue();
        assertThat(attributes)
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, "trusted-tenant")
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR, 1001L)
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.CLIENT_ID_ATTR, "trusted-client")
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.PROFILE_ATTR, Map.of("realm", "admin"));
    }

    @Test
    void authenticatedRequestWithoutTicketKeepsExistingIdentityFlow() {
        RealtimeConnectionTicketService ticketService = new RealtimeConnectionTicketService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("tenantId", "authenticated-tenant");
        request.setAttribute("userId", 1002L);
        request.addHeader("X-Mango-Tenant-Id", "spoofed-tenant");
        request.addHeader("X-Mango-User-Id", "9999");
        request.addHeader("X-Mango-Client-Id", "authenticated-client");
        Map<String, Object> attributes = new HashMap<>();

        assertThat(handshake(ticketService, request, new MockHttpServletResponse(), attributes)).isTrue();
        assertThat(attributes)
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, "authenticated-tenant")
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR, 1002L)
                .containsEntry(RealtimeWebSocketHandshakeInterceptor.CLIENT_ID_ATTR, "authenticated-client");
    }

    @Test
    void unknownOrExpiredTicketIsRejectedAsUnauthorized() {
        MutableClock clock = new MutableClock(1_000L);
        RealtimeConnectionTicketService ticketService = new RealtimeConnectionTicketService(clock, 100L);
        RealtimeConnectionTicket ticket = ticketService.issue("tenant-a", 1001L, "client-a", Map.of());

        assertRejected(ticketService, "unknown");
        clock.setMillis(ticket.expiresAt());
        assertRejected(ticketService, ticket.value());
    }

    private void assertRejected(RealtimeConnectionTicketService ticketService, String ticketValue) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("rtTicket", ticketValue);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(handshake(ticketService, request, response, new HashMap<>())).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private boolean handshake(RealtimeConnectionTicketService ticketService,
                              MockHttpServletRequest request,
                              MockHttpServletResponse response,
                              Map<String, Object> attributes) {
        return new RealtimeWebSocketHandshakeInterceptor(ticketService).beforeHandshake(
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(response),
                new ProbeWebSocketHandler(),
                attributes);
    }

    private static final class MutableClock extends Clock {

        private long millis;

        private MutableClock(long millis) {
            this.millis = millis;
        }

        private void setMillis(long millis) {
            this.millis = millis;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }
}
