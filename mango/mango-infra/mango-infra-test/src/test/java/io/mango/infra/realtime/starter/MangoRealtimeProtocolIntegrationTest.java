package io.mango.infra.realtime.starter;

import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimePollingService;
import io.mango.infra.realtime.api.RealtimePublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = MangoRealtimeProtocolIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude="
                        + "io.mango.infra.redis.starter.RedisAutoConfiguration,"
                        + "io.mango.infra.kv.starter.KvStoreAutoConfiguration,"
                        + "io.mango.infra.db.starter.DbAutoConfiguration,"
                        + "io.mango.infra.db.starter.MangoFlywayAutoConfiguration"
        })
class MangoRealtimeProtocolIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RealtimePublisher realtimePublisher;

    @Autowired
    private RealtimePollingService realtimePollingService;

    @Test
    void websocket_clientReceivesUserMessage() throws Exception {
        BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
        WebSocketSession session = connectWebSocketClient(receivedMessages);

        String connectedPayload = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(connectedPayload);
        assertTrue(connectedPayload.contains("WebSocket connected"));
        assertTrue(connectedPayload.contains("\"type\":\"connected\""));

        realtimePublisher.publishToUser(1001L, "message", "ws-user-message");

        String pushedPayload = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(pushedPayload);
        assertTrue(pushedPayload.contains("ws-user-message"));
        assertTrue(pushedPayload.contains("\"type\":\"message\""));

        session.close();
    }

    @Test
    void sse_clientReceivesUserMessage() throws Exception {
        BlockingQueue<String> receivedEvents = new LinkedBlockingQueue<>();
        WebClient.ResponseSpec responseSpec = WebClient.create()
                .get()
                .uri("http://localhost:" + port + "/realtime/subscribe?userId=2002")
                .header("Authorization", "Bearer test-token")
                .header("TENANT-ID", "tenant-a")
                .retrieve();

        var subscription = responseSpec
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .map(ServerSentEvent::data)
                .filter(data -> data != null && !data.isBlank())
                .subscribe(receivedEvents::offer);

        try {
            String connectedPayload = receivedEvents.poll(5, TimeUnit.SECONDS);
            assertNotNull(connectedPayload);
            assertTrue(connectedPayload.contains("SSE connected"));
            assertTrue(connectedPayload.contains("\"type\":\"connected\""));

            realtimePublisher.publishToUser(2002L, "message", "sse-user-message");

            String pushedPayload = receivedEvents.poll(5, TimeUnit.SECONDS);
            assertNotNull(pushedPayload);
            assertTrue(pushedPayload.contains("sse-user-message"));
            assertTrue(pushedPayload.contains("\"type\":\"message\""));
        } finally {
            subscription.dispose();
        }
    }

    @Test
    void polling_clientReceivesQueuedMessages() {
        realtimePollingService.append("client:3003", RealtimeMessage.of("message", "polling-message-1"));
        realtimePollingService.append("client:3003", RealtimeMessage.of("message", "polling-message-2"));

        List<RealtimeMessage> polledMessages = realtimePollingService.poll("client:3003", 10);
        List<RealtimeMessage> drainedMessages = realtimePollingService.poll("client:3003", 10);

        assertEquals(2, polledMessages.size());
        assertEquals("polling-message-1", polledMessages.get(0).content());
        assertEquals("polling-message-2", polledMessages.get(1).content());
        assertTrue(drainedMessages.isEmpty());
    }

    @Test
    void pollingEndpoint_shortPollingReturnsQueuedUserMessages() {
        realtimePublisher.publishToUser(3004L, "message", "polling-http-message");

        String payload = WebClient.create()
                .get()
                .uri("http://localhost:" + port + "/realtime/poll?userId=3004&maxSize=10")
                .header("Authorization", "Bearer test-token")
                .header("TENANT-ID", "tenant-a")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));

        assertNotNull(payload);
        assertTrue(payload.contains("polling-http-message"));
        assertTrue(payload.contains("\"type\":\"message\""));
    }

    @Test
    void pollingEndpoint_longPollingWaitsUntilMessageArrives() throws Exception {
        CompletableFuture<String> pendingResponse = WebClient.create()
                .get()
                .uri("http://localhost:" + port + "/realtime/poll?userId=3005&maxSize=10&timeoutMillis=3000")
                .header("Authorization", "Bearer test-token")
                .header("TENANT-ID", "tenant-a")
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();

        TimeUnit.MILLISECONDS.sleep(200);
        realtimePublisher.publishToUser(3005L, "message", "polling-long-message");

        String payload = pendingResponse.get(5, TimeUnit.SECONDS);
        assertNotNull(payload);
        assertTrue(payload.contains("polling-long-message"));
    }

    @Test
    void negotiate_returnsEnabledTransportsAndHonorsPreference() {
        String payload = WebClient.create()
                .get()
                .uri("http://localhost:" + port + "/realtime/negotiate?prefer=polling,sse,websocket")
                .header("Authorization", "Bearer test-token")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));

        assertNotNull(payload);
        assertTrue(payload.contains("\"recommended\":\"polling\""));
        assertTrue(payload.contains("\"type\":\"websocket\""));
        assertTrue(payload.contains("\"endpoint\":\"/realtime/ws\""));
        assertTrue(payload.contains("\"type\":\"sse\""));
        assertTrue(payload.contains("\"endpoint\":\"/realtime/subscribe\""));
        assertTrue(payload.contains("\"type\":\"polling\""));
        assertTrue(payload.contains("\"endpoint\":\"/realtime/poll\""));
    }

    private WebSocketSession connectWebSocketClient(BlockingQueue<String> receivedMessages) throws Exception {
        String url = "ws://localhost:" + port + "/realtime/ws?token=test-token&tenantId=tenant-a&userId=1001";
        StandardWebSocketClient client = new StandardWebSocketClient();
        return client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                receivedMessages.offer(message.getPayload());
            }
        }, url).get(5, TimeUnit.SECONDS);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
