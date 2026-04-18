package io.mango.infra.realtime.core.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.realtime.api.RealtimeInboundMessage;
import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.core.inbound.RealtimeInboundTransport;
import io.mango.infra.realtime.core.session.InMemoryRealtimeSubscriptionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeWebSocketHandlerTest {

    private InMemoryRealtimeSubscriptionManager subscriptionManager;
    private RealtimeWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        subscriptionManager = new InMemoryRealtimeSubscriptionManager();
        handler = new RealtimeWebSocketHandler(subscriptionManager, new ObjectMapper());
    }

    @Test
    void afterConnectionEstablished_validSession_subscribesAndSendsConnectedMessage() throws Exception {
        WebSocketSession session = newSession("s1", "tenant-a", 1L);

        handler.afterConnectionEstablished(session);

        assertEquals(1, subscriptionManager.findByTenant("tenant-a").size());
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_ping_sendsPong() throws Exception {
        WebSocketSession session = newSession("s1", "tenant-a", 1L);
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"ping\"}"));

        verify(session, org.mockito.Mockito.times(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_invalidPayload_sendsError() throws Exception {
        WebSocketSession session = newSession("s1", "tenant-a", 1L);

        handler.handleTextMessage(session, new TextMessage("not-json"));

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_businessPayload_forwardsInboundMessage() throws Exception {
        RecordingInboundTransport inboundTransport = new RecordingInboundTransport();
        handler = new RealtimeWebSocketHandler(subscriptionManager, new ObjectMapper(), inboundTransport, 1024);
        WebSocketSession session = newSession("s1", "tenant-a", 1L);

        handler.handleTextMessage(session,
                new TextMessage("{\"id\":\"m1\",\"type\":\"task.cancel\",\"content\":\"{}\","
                        + "\"headers\":{\"source\":\"panel\"}}"));

        RealtimeInboundMessage inboundMessage = inboundTransport.message;
        assertNotNull(inboundMessage);
        assertEquals("m1", inboundMessage.id());
        assertEquals("task.cancel", inboundMessage.type());
        assertEquals("{}", inboundMessage.content());
        assertEquals("tenant-a", inboundMessage.tenantId());
        assertEquals(1L, inboundMessage.userId());
        assertEquals("s1", inboundMessage.sessionId());
        assertEquals("panel", inboundMessage.headers().get("source"));
    }

    @Test
    void afterConnectionClosed_existingSession_unsubscribes() throws Exception {
        WebSocketSession session = newSession("s1", "tenant-a", 1L);
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertEquals(0, subscriptionManager.findAll().size());
    }

    @Test
    void sendToUser_existingSession_sendsEnvelope() throws Exception {
        WebSocketSession session = newSession("s1", "tenant-a", 1L);
        handler.afterConnectionEstablished(session);

        handler.sendToUser(1L, RealtimeMessage.toUser(1L, "message", "payload"));

        verify(session, org.mockito.Mockito.times(2)).sendMessage(any(TextMessage.class));
    }

    private WebSocketSession newSession(String id, String tenantId, Long userId) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, tenantId);
        attributes.put(RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR, userId);
        when(session.getId()).thenReturn(id);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static class RecordingInboundTransport implements RealtimeInboundTransport {

        private RealtimeInboundMessage message;

        @Override
        public void forward(RealtimeInboundMessage message) {
            this.message = message;
        }
    }
}
