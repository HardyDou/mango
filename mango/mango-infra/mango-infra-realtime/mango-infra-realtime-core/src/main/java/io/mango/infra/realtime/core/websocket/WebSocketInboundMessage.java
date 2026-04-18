package io.mango.infra.realtime.core.websocket;

import java.util.Map;

/**
 * Minimal inbound WebSocket command accepted by the realtime adapter.
 */
public record WebSocketInboundMessage(
        String id,
        String type,
        String content,
        Map<String, Object> headers) {
}
