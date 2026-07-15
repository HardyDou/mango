package io.mango.infra.realtime.starter.controller;

import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.inbound.forward.RealtimeInboundForwardServices;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicket;
import io.mango.infra.realtime.core.negotiate.RealtimeConnectionTicketService;
import io.mango.infra.realtime.core.polling.InMemoryRealtimePollingService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
