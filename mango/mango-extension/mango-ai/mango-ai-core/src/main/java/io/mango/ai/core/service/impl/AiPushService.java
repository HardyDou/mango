package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.core.service.IAiPushService;
import io.mango.common.result.Require;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;

/**
 * AI 模块进程内推送服务。
 */
@Service
public class AiPushService implements IAiPushService {

    private final ObjectMapper objectMapper;
    private final Duration heartbeatInterval;
    private final Sinks.Many<String> events = Sinks.many().multicast().directBestEffort();

    public AiPushService(
            ObjectMapper objectMapper,
            @Value("${mango.ai.sse.heartbeat-interval:25000}") long heartbeatIntervalMillis) {
        this.objectMapper = objectMapper.copy();
        this.heartbeatInterval = Duration.ofMillis(Math.max(heartbeatIntervalMillis, 1L));
    }

    @Override
    public Flux<String> connect() {
        Flux<String> heartbeat = Flux.interval(heartbeatInterval).map(ignored -> ":heartbeat");
        return Flux.concat(Flux.just(jsonEvent("connected", "SSE connected")), Flux.merge(events.asFlux(), heartbeat));
    }

    @Override
    public void broadcastNotification(String content) {
        events.tryEmitNext(jsonEvent("notification", content));
    }

    @Override
    public void broadcastAlert(String content) {
        events.tryEmitNext(jsonEvent("alert", content));
    }

    private String jsonEvent(String type, String content) {
        try {
            return objectMapper.writeValueAsString(Map.of("type", type, "content", content));
        } catch (JsonProcessingException exception) {
            return Require.rethrow(
                    new IllegalStateException("Failed to serialize AI push event", exception));
        }
    }
}
