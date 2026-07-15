package io.mango.infra.realtime.core.session;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.presence.InMemoryRealtimePresenceService;
import io.mango.infra.realtime.core.presence.RealtimeNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRealtimeSubscriptionManagerTest {

    @Test
    void defaultTenantSessionIsVisibleThroughTenantIndex() {
        InMemoryRealtimeSubscriptionManager manager = manager(new InMemoryRealtimePresenceService());
        manager.subscribe(new TestSession("s1", null, 1001L, null));

        assertThat(manager.findByTenant(null)).extracting(RealtimeSession::id).containsExactly("s1");
        assertThat(manager.countByTenant("default")).isEqualTo(1);
    }

    @Test
    void replacingSessionRemovesPreviousGroupMembershipFromAllIndexes() {
        InMemoryRealtimePresenceService presenceService = new InMemoryRealtimePresenceService();
        InMemoryRealtimeSubscriptionManager manager = manager(presenceService);
        manager.subscribe(new TestSession("s1", "tenant-a", 1001L, "old-client"));
        manager.subscribeGroup("s1", "old-room");

        manager.subscribe(new TestSession("s1", "tenant-b", 2002L, "new-client"));

        assertThat(manager.findByGroup("tenant-a", "old-room")).isEmpty();
        assertThat(presenceService.findByGroup("tenant-a", "old-room")).isEmpty();
        assertThat(manager.findByTenant("tenant-a")).isEmpty();
        assertThat(manager.findByTenant("tenant-b")).extracting(RealtimeSession::id).containsExactly("s1");
    }

    private InMemoryRealtimeSubscriptionManager manager(InMemoryRealtimePresenceService presenceService) {
        return new InMemoryRealtimeSubscriptionManager(
                presenceService,
                new RealtimeNode("node-a", "realtime", "/", "/_realtime/messages/outbound"));
    }

    private record TestSession(String id, String tenantId, Long userId, String clientId) implements RealtimeSession {
        @Override
        public String protocol() {
            return "TEST";
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void send(RealtimeOutboundMessage envelope) {
        }
    }
}
