package io.mango.message.core.channel.impl;

import io.mango.infra.sse.service.SseService;
import io.mango.message.core.channel.MessageChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SSE message channel.
 * Uses the shared SseService from mango-infra-sse.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseChannel implements MessageChannel {

    private final SseService sseService;

    @Override
    public void sendToUser(Long userId, String message) {
        try {
            sseService.sendToTenant(String.valueOf(userId), "message", message);
        } catch (Exception e) {
            log.error("Failed to send SSE message to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void broadcast(String message) {
        try {
            sseService.broadcast("message", message);
        } catch (Exception e) {
            log.error("Failed to broadcast SSE message: {}", e.getMessage());
        }
    }

    @Override
    public String getType() {
        return "SSE";
    }
}
