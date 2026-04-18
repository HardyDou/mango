package io.mango.infra.realtime.core.sse;

import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimeProtocols;
import io.mango.infra.realtime.api.RealtimeSession;
import io.mango.infra.realtime.api.RealtimeSubscriptionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SseProtocolAdapter and SseRealtimeSession.
 * No Spring context required.
 */
class SseProtocolAdapterTest {

    private RecordingSubscriptionManager subscriptionManager;
    private SseProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        subscriptionManager = new RecordingSubscriptionManager();
        adapter = new SseProtocolAdapter(subscriptionManager, 30_000L);
    }

    // ===== protocol() =====

    @Test
    void protocol_returnsSse() {
        assertEquals(RealtimeProtocols.SSE, adapter.protocol());
    }

    // ===== createEmitter =====

    @Test
    void createEmitter_subscribesSession() {
        SseEmitter emitter = adapter.createEmitter("tenant-a", 1L);

        assertNotNull(emitter);
        assertEquals(1, subscriptionManager.subscribedSessions.size());
        RealtimeSession session = subscriptionManager.subscribedSessions.get(0);
        assertEquals("tenant-a", session.tenantId());
        assertEquals(1L, session.userId());
        assertEquals(RealtimeProtocols.SSE, session.protocol());
    }

    @Test
    void createEmitter_nullTenant_defaultsToDefault() {
        adapter.createEmitter(null, 1L);

        assertEquals("default", subscriptionManager.subscribedSessions.get(0).tenantId());
    }

    @Test
    void createEmitter_blankTenant_defaultsToDefault() {
        adapter.createEmitter("  ", 1L);

        assertEquals("default", subscriptionManager.subscribedSessions.get(0).tenantId());
    }

    @Test
    void createEmitter_nullUserId_allowed() {
        adapter.createEmitter("tenant-a", null);

        assertNull(subscriptionManager.subscribedSessions.get(0).userId());
    }

    @Test
    void createEmitter_multipleSessions_allSubscribed() {
        adapter.createEmitter("tenant-a", 1L);
        adapter.createEmitter("tenant-a", 2L);

        assertEquals(2, subscriptionManager.subscribedSessions.size());
    }

    @Test
    void createEmitter_closeCallback_removesSession() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        adapter = new TestableSseProtocolAdapter(subscriptionManager, emitter);
        adapter.createEmitter("tenant-a", 1L);
        SseRealtimeSession session = (SseRealtimeSession) subscriptionManager.subscribedSessions.get(0);

        emitter.completeCallback.run();

        assertEquals(1, subscriptionManager.unsubscribedSessionIds.size());
        assertEquals(session.id(), subscriptionManager.unsubscribedSessionIds.get(0));
    }

    // ===== sendToUser =====

    @Test
    void sendToUser_noMatchingSession_noError() {
        // Should not throw even though no session matches
        adapter.sendToUser(999L, RealtimeMessage.of("type", "content"));
    }

    @Test
    void sendToUser_matchingSession_sendsMessage() {
        RecordingSession session = new RecordingSession("s1", RealtimeProtocols.SSE, "tenant-a", 1L);
        subscriptionManager.subscribe(session);

        RealtimeMessage message = RealtimeMessage.toUser(1L, "notice", "hello");
        adapter.sendToUser(1L, message);

        assertEquals(message, session.lastMessage);
    }

    @Test
    void sendToUser_differentProtocolSession_notSent() {
        // Create a non-SSE session directly in manager
        subscriptionManager.subscribe(new NonSseTestSession("s1", "tenant-a", 1L));

        adapter.sendToUser(1L, RealtimeMessage.toUser(1L, "notice", "hello"));

        // Should not throw - NonSseTestSession.send() would fail if called
    }

    // ===== sendToTenant =====

    @Test
    void sendToTenant_matchingTenantAndProtocol_sendsOnlyEligibleSessions() {
        RecordingSession tenantA = new RecordingSession("s1", RealtimeProtocols.SSE, "tenant-a", 1L);
        RecordingSession tenantB = new RecordingSession("s2", RealtimeProtocols.SSE, "tenant-b", 2L);
        RecordingSession websocket = new RecordingSession("s3", RealtimeProtocols.WEBSOCKET, "tenant-a", 3L);
        subscriptionManager.subscribe(tenantA);
        subscriptionManager.subscribe(tenantB);
        subscriptionManager.subscribe(websocket);

        RealtimeMessage message = RealtimeMessage.toTenant("tenant-a", "notice", "broadcast");
        adapter.sendToTenant("tenant-a", message);

        assertEquals(message, tenantA.lastMessage);
        assertNull(tenantB.lastMessage);
        assertNull(websocket.lastMessage);
    }

    // ===== broadcast =====

    @Test
    void broadcast_noSessions_noError() {
        // Should not throw
        adapter.broadcast(RealtimeMessage.of("type", "content"));
    }

    @Test
    void broadcast_sendsOnlySseSessions() {
        RecordingSession sse = new RecordingSession("s1", RealtimeProtocols.SSE, "tenant-a", 1L);
        RecordingSession websocket = new RecordingSession("s2", RealtimeProtocols.WEBSOCKET, "tenant-a", 1L);
        subscriptionManager.subscribe(sse);
        subscriptionManager.subscribe(websocket);

        RealtimeMessage message = RealtimeMessage.of("notice", "broadcast");
        adapter.broadcast(message);

        assertEquals(message, sse.lastMessage);
        assertNull(websocket.lastMessage);
    }

    // ===== SseRealtimeSession =====

    @Test
    void sseSession_isOpen_trueWhenEmitterNotNull() {
        SseEmitter emitter = adapter.createEmitter("tenant-a", 1L);
        RealtimeSession session = subscriptionManager.subscribedSessions.get(0);

        assertTrue(session.isOpen());
    }

    @Test
    void sseSession_send_wrongTypeContent_isOk() throws Exception {
        SseEmitter emitter = adapter.createEmitter("tenant-a", 1L);
        RealtimeSession session = subscriptionManager.subscribedSessions.get(0);

        // Sending should not throw even if content type seems wrong
        session.send(RealtimeMessage.toUser(1L, "notice", "content"));
    }

    @Test
    void sseSession_send_nullContent_isOk() throws Exception {
        SseEmitter emitter = adapter.createEmitter("tenant-a", 1L);
        RealtimeSession session = subscriptionManager.subscribedSessions.get(0);

        session.send(RealtimeMessage.of("notice", null));
    }

    // ===== Test helpers =====

    private static final class TestableSseProtocolAdapter extends SseProtocolAdapter {

        private final SseEmitter emitter;

        TestableSseProtocolAdapter(RealtimeSubscriptionManager subscriptionManager, SseEmitter emitter) {
            super(subscriptionManager, 30_000L);
            this.emitter = emitter;
        }

        @Override
        protected SseEmitter createSseEmitter() {
            return emitter;
        }
    }

    private static class CapturingSseEmitter extends SseEmitter {

        private Runnable completeCallback = () -> {};

        @Override
        public synchronized void onCompletion(Runnable callback) {
            this.completeCallback = callback;
        }
    }

    private static class RecordingSubscriptionManager implements RealtimeSubscriptionManager {
        final List<RealtimeSession> subscribedSessions = new ArrayList<>();
        final List<String> unsubscribedSessionIds = new ArrayList<>();

        @Override
        public void subscribe(RealtimeSession session) {
            subscribedSessions.add(session);
        }

        @Override
        public void unsubscribe(String sessionId) {
            unsubscribedSessionIds.add(sessionId);
        }

        @Override
        public List<RealtimeSession> findByTenant(String tenantId) {
            return subscribedSessions.stream()
                    .filter(s -> s.tenantId().equals(tenantId) && s.isOpen())
                    .toList();
        }

        @Override
        public List<RealtimeSession> findByUser(Long userId) {
            return subscribedSessions.stream()
                    .filter(s -> java.util.Objects.equals(userId, s.userId()) && s.isOpen())
                    .toList();
        }

        @Override
        public List<RealtimeSession> findAll() {
            return subscribedSessions.stream().filter(RealtimeSession::isOpen).toList();
        }

        @Override
        public int countByTenant(String tenantId) {
            return (int) findByTenant(tenantId).size();
        }
    }

    private static class NonSseTestSession implements RealtimeSession {
        private final String id;
        private final String tenantId;
        private final Long userId;

        NonSseTestSession(String id, String tenantId, Long userId) {
            this.id = id;
            this.tenantId = tenantId;
            this.userId = userId;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String protocol() {
            return "OTHER_PROTOCOL";
        }

        @Override
        public String tenantId() {
            return tenantId;
        }

        @Override
        public Long userId() {
            return userId;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void send(RealtimeMessage envelope) {
            fail("Non-SSE session should not receive SSE broadcast");
        }
    }

    private static class RecordingSession implements RealtimeSession {
        private final String id;
        private final String protocol;
        private final String tenantId;
        private final Long userId;
        private RealtimeMessage lastMessage;

        RecordingSession(String id, String protocol, String tenantId, Long userId) {
            this.id = id;
            this.protocol = protocol;
            this.tenantId = tenantId;
            this.userId = userId;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String protocol() {
            return protocol;
        }

        @Override
        public String tenantId() {
            return tenantId;
        }

        @Override
        public Long userId() {
            return userId;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void send(RealtimeMessage envelope) {
            lastMessage = envelope;
        }
    }
}
