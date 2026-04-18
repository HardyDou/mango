package io.mango.infra.realtime.core.session;

import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimeSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRealtimeSubscriptionManagerTest {

    @Test
    void subscribe_multipleSessions_indexesByTenantAndUser() {
        InMemoryRealtimeSubscriptionManager manager = new InMemoryRealtimeSubscriptionManager();
        manager.subscribe(new TestSession("s1", "SSE", "tenant-a", 1L, true));
        manager.subscribe(new TestSession("s2", "WEBSOCKET", "tenant-a", 2L, true));
        manager.subscribe(new TestSession("s3", "SSE", "tenant-b", 1L, true));

        assertEquals(2, manager.findByTenant("tenant-a").size());
        assertEquals(2, manager.countByTenant("tenant-a"));
        assertEquals(2, manager.findByUser(1L).size());
        assertEquals(3, manager.findAll().size());
    }

    @Test
    void findMethods_closedSessions_areExcluded() {
        InMemoryRealtimeSubscriptionManager manager = new InMemoryRealtimeSubscriptionManager();
        manager.subscribe(new TestSession("s1", "SSE", "tenant-a", 1L, false));
        manager.subscribe(new TestSession("s2", "SSE", "tenant-a", 2L, true));

        assertEquals(1, manager.findByTenant("tenant-a").size());
        assertEquals(1, manager.countByTenant("tenant-a"));
        assertEquals(0, manager.findByUser(1L).size());
    }

    @Test
    void unsubscribe_existingSession_removesIndexes() {
        InMemoryRealtimeSubscriptionManager manager = new InMemoryRealtimeSubscriptionManager();
        manager.subscribe(new TestSession("s1", "SSE", "tenant-a", 1L, true));

        manager.unsubscribe("s1");

        assertTrue(manager.findAll().isEmpty());
        assertEquals(0, manager.countByTenant("tenant-a"));
    }

    @Test
    void subscribe_sameSessionId_replacesTenantIndex() {
        InMemoryRealtimeSubscriptionManager manager = new InMemoryRealtimeSubscriptionManager();
        manager.subscribe(new TestSession("s1", "SSE", "tenant-a", 1L, true));
        manager.subscribe(new TestSession("s1", "SSE", "tenant-b", 1L, true));

        assertEquals(0, manager.countByTenant("tenant-a"));
        assertEquals(1, manager.countByTenant("tenant-b"));
        assertTrue(manager.findByTenant("tenant-a").isEmpty());
    }

    private record TestSession(
            String id,
            String protocol,
            String tenantId,
            Long userId,
            boolean isOpen) implements RealtimeSession {

        @Override
        public void send(RealtimeMessage envelope) {
            // No-op test session.
        }
    }
}
