package io.mango.infra.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.websocket.config.WebSocketHandshakeInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket handler for real-time chat messages
 * <p>
 * Supports ping/pong heartbeat and tenant isolation.
 *
 * @author Mango
 */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    /**
     * Tenant to WebSocket sessions mapping
     */
    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> tenantSessions = new ConcurrentHashMap<>();

    /**
     * User ID to WebSocket session mapping (for direct user messaging)
     */
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String tenantId = getTenantId(session);
        if (tenantId == null) {
            tenantId = "default";
        }

        tenantSessions.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(session);

        // Store user-session mapping if userId is available
        Long userId = getUserId(session);
        if (userId != null) {
            userSessions.put(userId, session);
        }

        log.info("WebSocket connection established for tenant: {}, total connections: {}",
                tenantId, getConnectionCount(tenantId));

        // Send connection success message
        sendMessage(session, "connected", "WebSocket connected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String tenantId = getTenantId(session);
        if (tenantId == null) {
            tenantId = "default";
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");

            if ("ping".equals(type)) {
                // Heartbeat response
                sendMessage(session, "pong", null);
            } else if ("message".equals(type)) {
                // Echo message back or broadcast
                String content = (String) data.get("content");
                log.debug("Received message from tenant {}: {}", tenantId, content);

                // Broadcast to all sessions in the same tenant
                broadcast(tenantId, "message", content);
            }
        } catch (Exception e) {
            log.warn("Failed to process WebSocket message", e);
            sendMessage(session, "error", "Invalid message format");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String tenantId = getTenantId(session);
        if (tenantId == null) {
            tenantId = "default";
        }

        tenantSessions.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).remove(session);
        // Atomic remove-if-empty to avoid race condition
        tenantSessions.computeIfPresent(tenantId, (k, v) -> v.isEmpty() ? null : v);

        // Clean up user-session mapping
        Long userId = getUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
        }

        log.info("WebSocket connection closed for tenant: {}, reason: {}, remaining connections: {}",
                tenantId, status, getConnectionCount(tenantId));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String tenantId = getTenantId(session);
        if (tenantId == null) {
            tenantId = "default";
        }

        log.warn("WebSocket transport error for tenant: {}", tenantId, exception);

        // Close the session on error
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            log.warn("Failed to close WebSocket session", e);
        }
    }

    /**
     * Send message to a specific session
     */
    private void sendMessage(WebSocketSession session, String type, String content) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "content", content != null ? content : ""
            ));
            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            log.warn("Failed to send WebSocket message", e);
        }
    }

    /**
     * Broadcast message to all sessions in a tenant
     */
    private void broadcast(String tenantId, String type, String content) {
        CopyOnWriteArrayList<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "content", content != null ? content : ""
            ));

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.warn("Failed to broadcast message to session: {}", session.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast message", e);
        }
    }

    /**
     * Get tenant ID from session attributes
     */
    private String getTenantId(WebSocketSession session) {
        Object tenantId = session.getAttributes().get(WebSocketHandshakeInterceptor.TENANT_ID_ATTR);
        return tenantId != null ? tenantId.toString() : null;
    }

    /**
     * Get connection count for a tenant
     */
    public int getConnectionCount(String tenantId) {
        CopyOnWriteArrayList<WebSocketSession> sessions = tenantSessions.get(tenantId);
        return sessions == null ? 0 : sessions.size();
    }

    /**
     * Get user ID from session attributes
     */
    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(WebSocketHandshakeInterceptor.USER_ID_ATTR);
        if (userId == null) {
            return null;
        }
        if (userId instanceof Long) {
            return (Long) userId;
        }
        return Long.parseLong(userId.toString());
    }

    /**
     * Send message to a specific user
     */
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, "message", message);
        } else {
            log.warn("Cannot send message to user {}: session not found or closed", userId);
        }
    }

    /**
     * Broadcast message to all sessions (for backwards compatibility)
     */
    public void broadcast(String message) {
        // Broadcast to default tenant
        broadcastToTenant("default", message);
    }

    /**
     * Broadcast message to a specific tenant
     */
    public void broadcastToTenant(String tenantId, String message) {
        CopyOnWriteArrayList<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String msg = objectMapper.writeValueAsString(Map.of(
                    "type", "broadcast",
                    "content", message != null ? message : ""
            ));

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(msg));
                    } catch (IOException e) {
                        log.warn("Failed to broadcast message to session: {}", session.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast message", e);
        }
    }
}
