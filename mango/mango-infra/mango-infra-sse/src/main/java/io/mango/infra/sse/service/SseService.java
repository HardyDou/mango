package io.mango.infra.sse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE service for server-sent events push
 * <p>
 * Manages SSE connections per tenant with heartbeat support.
 *
 * @author Mango
 */
@Slf4j
@Service
public class SseService {

    /**
     * SSE connection timeout in milliseconds (5 minutes)
     */
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    /**
     * Tenant to SSE emitters mapping
     */
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();

    /**
     * Create SSE emitter for a tenant
     *
     * @param tenantId tenant identifier
     * @return SseEmitter instance
     */
    public SseEmitter createEmitter(String tenantId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        tenantEmitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for tenant: {}", tenantId);
            removeEmitter(tenantId, emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE connection timeout for tenant: {}", tenantId);
            removeEmitter(tenantId, emitter);
        });

        emitter.onError(e -> {
            log.warn("SSE connection error for tenant: {}", tenantId, e);
            removeEmitter(tenantId, emitter);
        });

        log.info("SSE emitter created for tenant: {}, total connections: {}",
                tenantId, getConnectionCount(tenantId));

        return emitter;
    }

    /**
     * Send message to all connections of a specific tenant
     *
     * @param tenantId tenant identifier
     * @param type     message type (notification/alert)
     * @param content  message content
     */
    public void sendToTenant(String tenantId, String type, String content) {
        CopyOnWriteArrayList<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No SSE connections for tenant: {}", tenantId);
            return;
        }

        String message = String.format("{\"type\":\"%s\",\"content\":\"%s\"}", type, content);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message));
            } catch (IOException e) {
                log.warn("Failed to send SSE message to tenant: {}", tenantId, e);
                removeEmitter(tenantId, emitter);
            }
        }
    }

    /**
     * Send heartbeat to all connections of a specific tenant
     *
     * @param tenantId tenant identifier
     */
    public void sendHeartbeat(String tenantId) {
        CopyOnWriteArrayList<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String heartbeat = "{\"type\":\"pong\"}";
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(heartbeat));
            } catch (IOException e) {
                log.warn("Failed to send heartbeat to tenant: {}", tenantId, e);
                removeEmitter(tenantId, emitter);
            }
        }
    }

    /**
     * Broadcast message to all tenants
     *
     * @param type    message type
     * @param content message content
     */
    public void broadcast(String type, String content) {
        String message = String.format("{\"type\":\"%s\",\"content\":\"%s\"}", type, content);
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : tenantEmitters.entrySet()) {
            String tenantId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(message));
                } catch (IOException e) {
                    log.warn("Failed to broadcast to tenant: {}", tenantId, e);
                    removeEmitter(tenantId, emitter);
                }
            }
        }
    }

    /**
     * Get connection count for a tenant
     *
     * @param tenantId tenant identifier
     * @return number of active connections
     */
    public int getConnectionCount(String tenantId) {
        CopyOnWriteArrayList<SseEmitter> emitters = tenantEmitters.get(tenantId);
        return emitters == null ? 0 : emitters.size();
    }

    /**
     * Remove emitter from tenant's connection list
     */
    private void removeEmitter(String tenantId, SseEmitter emitter) {
        tenantEmitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).remove(emitter);
        // Check if empty after removal using compute to avoid race condition
        tenantEmitters.computeIfPresent(tenantId, (k, v) -> v.isEmpty() ? null : v);
        log.info("SSE emitter removed for tenant: {}, remaining connections: {}",
                tenantId, getConnectionCount(tenantId));
    }
}
