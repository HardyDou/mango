package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.inbound.forward.RealtimeInboundForwardServices;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicket;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicketService;
import io.mango.infra.realtime.core.polling.InMemoryRealtimePollingService;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.sse.SseProtocolAdapter;
import io.mango.infra.realtime.core.sse.SseRealtimeSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeControllerSecurityTest {

    @Test
    void negotiationTicketUsesAuthenticatedIdentityInsteadOfSpoofedHeaders() {
        RealtimeConnectionTicketService ticketService = new RealtimeConnectionTicketService();
        RealtimeNegotiationController controller = new RealtimeNegotiationController(List.of(), ticketService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("tenantId", "trusted-tenant");
        request.setAttribute("userId", 1001L);
        request.addHeader("X-Mango-Tenant-Id", "spoofed-tenant");
        request.addHeader("X-Mango-User-Id", "9999");

        String ticketValue = controller.negotiate(
                null, true, true, true, true, true, true, "https", request).connectionTicket();
        RealtimeConnectionTicket ticket = ticketService.resolve(ticketValue).orElseThrow();

        assertThat(ticket.tenantId()).isEqualTo("trusted-tenant");
        assertThat(ticket.userId()).isEqualTo(1001L);
    }

    @Test
    void pollingProbeRejectsUnknownTicketAfterAuthBypass() {
        RealtimeConnectionTicketService ticketService = new RealtimeConnectionTicketService();
        PollingRealtimeController controller = new PollingRealtimeController(
                new InMemoryRealtimePollingService(),
                20,
                100,
                0L,
                25_000L,
                new ProtocolRealtimeInboundForwarder(RealtimeInboundForwardServices.noop()),
                ticketService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("rtTicket", "unknown");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            assertThatThrownBy(controller::probe)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired realtime ticket");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void sseConnectionTicketRestoresTrustedIdentityInsteadOfRequestIdentity() {
        RealtimeConnectionTicketService ticketService = new RealtimeConnectionTicketService();
        RealtimeConnectionTicket ticket = ticketService.issue(
                "trusted-tenant", 1001L, "trusted-client", java.util.Map.of());
        SseProtocolAdapter adapter = mock(SseProtocolAdapter.class);
        when(adapter.createSession("trusted-tenant", 1001L, "trusted-client"))
                .thenReturn(new SseRealtimeSession(
                        "session-1", "trusted-tenant", 1001L, "trusted-client", new SseEmitter(), null));
        SseRealtimeController controller = new SseRealtimeController(
                adapter,
                mock(ProtocolRealtimeInboundForwarder.class),
                ticketService,
                mock(RealtimeSubscriptionManager.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("tenantId", "spoofed-tenant");
        request.setAttribute("userId", 9999L);
        request.setParameter("clientId", "spoofed-client");

        controller.connect(null, null, null, "spoofed-tenant", 9999L, ticket.value(), request);

        verify(adapter).createSession("trusted-tenant", 1001L, "trusted-client");
    }

    @Test
    void sseConnectionRejectsUnknownTicketAsUnauthorized() {
        RealtimeConnectionTicketService ticketService = new RealtimeConnectionTicketService();
        SseRealtimeController controller = new SseRealtimeController(
                mock(SseProtocolAdapter.class),
                mock(ProtocolRealtimeInboundForwarder.class),
                ticketService,
                mock(RealtimeSubscriptionManager.class));

        assertThatThrownBy(() -> controller.connect(
                null, null, null, null, null, "unknown", new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
