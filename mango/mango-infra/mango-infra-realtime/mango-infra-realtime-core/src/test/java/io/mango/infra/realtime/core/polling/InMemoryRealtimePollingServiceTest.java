package io.mango.infra.realtime.core.polling;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRealtimePollingServiceTest {

    @Test
    void isolatesSameClientIdAcrossTenants() {
        InMemoryRealtimePollingService service = new InMemoryRealtimePollingService();
        String tenantASubscriber = InMemoryRealtimePollingService.clientSubscriberId("tenant-a", "browser-1");
        String tenantBSubscriber = InMemoryRealtimePollingService.clientSubscriberId("tenant-b", "browser-1");
        service.register(tenantASubscriber, "tenant-a", null, "browser-1");
        service.register(tenantBSubscriber, "tenant-b", null, "browser-1");

        service.publishToClient(
                "tenant-a", "browser-1", RealtimeOutboundMessage.toTenant("tenant-a", "message", "tenant-a-only"));

        assertThat(service.poll(tenantASubscriber, 10))
                .extracting(RealtimeOutboundMessage::content)
                .containsExactly("tenant-a-only");
        assertThat(service.poll(tenantBSubscriber, 10)).isEmpty();
    }

    @Test
    void replacesPreviousRegistrationIndexes() {
        InMemoryRealtimePollingService service = new InMemoryRealtimePollingService();
        String subscriberId = "subscriber-1";
        service.register(subscriberId, "tenant-a", 1001L, "old-client");
        service.register(subscriberId, "tenant-b", 2002L, "new-client");

        service.publishToClient(
                "tenant-a", "old-client", RealtimeOutboundMessage.toTenant("tenant-a", "message", "stale"));
        service.publishToClient(
                "tenant-b", "new-client", RealtimeOutboundMessage.toTenant("tenant-b", "message", "current"));

        assertThat(service.poll(subscriberId, 10))
                .extracting(RealtimeOutboundMessage::content)
                .containsExactly("current");
    }
}
