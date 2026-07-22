package io.mango.ai.starter;

import io.mango.ai.core.service.IAiPushService;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 通知经真实 Realtime SSE 传输的端到端测试。
 */
@Tag("flow")
@Tag("ai")
@SpringBootTest(
        classes = AiRealtimeE2eTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mango.infra.realtime.outbox.enabled=false",
                "server.shutdown=immediate"
        })
class AiRealtimeE2eTest {

    @LocalServerPort
    private int port;

    @Autowired
    private IAiPushService aiPushService;

    @Test
    void aiPushService_真实RealtimeSse连接收到通知与告警() throws Exception {
        BlockingQueue<String> receivedEvents = new LinkedBlockingQueue<>();
        var subscription = WebClient.create()
                .get()
                .uri("http://localhost:" + port + "/realtime/transports/sse?userId=567")
                .header("TENANT-ID", "tenant-567")
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .map(ServerSentEvent::data)
                .filter(data -> data != null && !data.isBlank())
                .subscribe(receivedEvents::offer);

        try {
            String connectedPayload = receivedEvents.poll(5, TimeUnit.SECONDS);
            assertNotNull(connectedPayload);
            assertTrue(connectedPayload.contains("SSE connected"), connectedPayload);

            aiPushService.broadcastNotification("issue-567-notification");
            aiPushService.broadcastAlert("issue-567-alert");

            String notificationPayload = receivedEvents.poll(5, TimeUnit.SECONDS);
            String alertPayload = receivedEvents.poll(5, TimeUnit.SECONDS);
            assertNotNull(notificationPayload);
            assertNotNull(alertPayload);
            assertTrue(notificationPayload.contains("\"name\":\"notification\""), notificationPayload);
            assertTrue(notificationPayload.contains("issue-567-notification"), notificationPayload);
            assertTrue(alertPayload.contains("\"name\":\"alert\""), alertPayload);
            assertTrue(alertPayload.contains("issue-567-alert"), alertPayload);
        } finally {
            subscription.dispose();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        MemoryKvStore memoryKvStore() {
            return new MemoryKvStore();
        }
    }
}
