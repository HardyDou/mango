package io.mango.ai.core.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE (Server-Sent Events) controller for server push capability.
 * <p>
 * Provides real-time notification delivery to connected clients.
 *
 * @author Mango
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class SseController {

    private static final long SSE_TIMEOUT = 0L; // No timeout
    private static final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private static final ScheduledExecutorService heartbeat =
        Executors.newSingleThreadScheduledExecutor();

    static {
        heartbeat.scheduleAtFixedRate(() -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            }
        }, 25, 25, TimeUnit.SECONDS);
    }

    /**
     * Establish SSE connection for server push.
     *
     * @param request HTTP request
     * @return SSE emitter for the connected client
     */
    @GetMapping("/sse")
    public SseEmitter connect(HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                .name("message")
                .data("{\"type\": \"connected\", \"content\": \"SSE connected\"}"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        log.info("SSE client connected, total clients: {}", emitters.size());
        return emitter;
    }

    /**
     * Broadcast a notification to all connected SSE clients.
     *
     * @param content notification content
     */
    public static void broadcastNotification(String content) {
        broadcast("notification", content);
    }

    /**
     * Broadcast an alert to all connected SSE clients.
     *
     * @param content alert content
     */
    public static void broadcastAlert(String content) {
        broadcast("alert", content);
    }

    /**
     * Internal broadcast method.
     */
    private static void broadcast(String type, String content) {
        String data = "{\"type\": \"" + type + "\", \"content\": \"" + content + "\"}";
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
