package io.mango.infra.realtime.core.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.realtime.api.RealtimeHeaders;
import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimeProtocols;
import io.mango.infra.realtime.api.RealtimeSession;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketRealtimeSession.
 */
class WebSocketRealtimeSessionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void id_delegatesToWsSession() {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("ws-session-123");

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        assertEquals("ws-session-123", session.id());
    }

    @Test
    void protocol_returnsWebSocket() {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        assertEquals(RealtimeProtocols.WEBSOCKET, session.protocol());
    }

    @Test
    void tenantId_fromSessionAttribute() {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.getAttributes()).thenReturn(Map.of(
                RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, "tenant-xyz"
        ));

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        assertEquals("tenant-xyz", session.tenantId());
    }

    @Test
    void tenantId_missingDefaultsToDefault() {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.getAttributes()).thenReturn(Map.of());

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        assertEquals("default", session.tenantId());
    }

    @Test
    void userId_LongFromAttributes() {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.getAttributes()).thenReturn(Map.of(
                RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, "tenant",
                RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR, 42L
        ));

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        assertEquals(42L, session.userId());
    }

    @Test
    void userId_StringParsedToLong() {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.getAttributes()).thenReturn(Map.of(
                RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, "tenant",
                RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR, "99"
        ));

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        assertEquals(99L, session.userId());
    }

    @Test
    void userId_invalidStringReturnsNull() {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.getAttributes()).thenReturn(Map.of(
                RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, "tenant",
                RealtimeWebSocketHandshakeInterceptor.USER_ID_ATTR, "not-a-number"
        ));

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        assertNull(session.userId());
    }

    @Test
    void userId_missingReturnsNull() {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.getAttributes()).thenReturn(Map.of(
                RealtimeWebSocketHandshakeInterceptor.TENANT_ID_ATTR, "tenant"
        ));

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        assertNull(session.userId());
    }

    @Test
    void isOpen_delegatesToWsSession() {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.isOpen()).thenReturn(true);

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        assertTrue(session.isOpen());

        when(ws.isOpen()).thenReturn(false);
        assertFalse(session.isOpen());
    }

    @Test
    void send_serializesEnvelopeAndSendsTextMessage() throws Exception {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.isOpen()).thenReturn(true);

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        RealtimeMessage msg = RealtimeMessage.toUser(1L, "notice", "hello world");

        session.send(msg);

        verify(ws).sendMessage(argThat((TextMessage m) ->
                m.getPayload().contains("\"type\":\"notice\"") &&
                m.getPayload().contains("hello world") &&
                m.getPayload().contains("\"userId\":1")
        ));
    }

    @Test
    void send_nullContent_serializes() throws Exception {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.isOpen()).thenReturn(true);

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);
        session.send(RealtimeMessage.of("notice", null));

        // Should not throw, verify sendMessage was called
        verify(ws).sendMessage(any(TextMessage.class));
    }

    @Test
    void send_wsSessionThrows_convertsToException() throws Exception {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("id");
        when(ws.isOpen()).thenReturn(true);
        doThrow(new java.io.IOException("send error"))
                .when(ws).sendMessage(any(TextMessage.class));

        WebSocketRealtimeSession session = new WebSocketRealtimeSession(ws, objectMapper);

        assertThrows(IllegalStateException.class, () ->
                session.send(RealtimeMessage.of("notice", "content")));
    }
}
