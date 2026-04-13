package io.mango.message.core.channel.impl;

import io.mango.infra.websocket.handler.WebSocketHandler;
import io.mango.message.core.channel.MessageChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket message channel.
 * Uses the shared WebSocketHandler from mango-infra-websocket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannel implements MessageChannel {

    private final WebSocketHandler webSocketHandler;

    @Override
    public void sendToUser(Long userId, String message) {
        try {
            webSocketHandler.sendToUser(userId, message);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void broadcast(String message) {
        try {
            webSocketHandler.broadcast(message);
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket message: {}", e.getMessage());
        }
    }

    @Override
    public String getType() {
        return "WEBSOCKET";
    }
}
