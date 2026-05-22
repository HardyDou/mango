package io.mango.infra.realtime.e2e;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeContext;
import io.mango.infra.realtime.api.dto.RealtimeEvent;
import io.mango.infra.realtime.api.dto.RealtimePayload;
import io.mango.infra.realtime.api.dto.RealtimeSource;
import io.mango.infra.realtime.core.inbound.receiver.IRealtimeInboundReceiverService;
import io.mango.infra.realtime.e2e.apps.local.listener.LocalPrimaryInboundListener;
import io.mango.infra.realtime.e2e.apps.local.listener.extra.LocalExtraInboundListener;
import io.mango.infra.realtime.e2e.apps.remote.listener.RemoteInboundListener;
import io.mango.infra.realtime.e2e.support.RealtimeTestApps;
import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MangoRealtimeInboundMultiServiceE2ETest {

    private static ConfigurableApplicationContext localContext;
    private static ConfigurableApplicationContext remoteContext;
    private static int localPort;
    private static int remotePort;

    @BeforeAll
    static void setUpContexts() {
        RealtimeTestApps.StartedApps startedApps = RealtimeTestApps.startLocalAndRemote();
        localContext = startedApps.localContext();
        remoteContext = startedApps.remoteContext();
        localPort = startedApps.localPort();
        remotePort = startedApps.remotePort();
    }

    @AfterAll
    static void tearDownContexts() {
        if (remoteContext != null) {
            remoteContext.close();
        }
        if (localContext != null) {
            localContext.close();
        }
    }

    @BeforeEach
    void resetListeners() {
        LocalPrimaryInboundListener.reset();
        LocalExtraInboundListener.reset();
        RemoteInboundListener.reset();
    }

    @Test
    void inboundFlow_scansRegistersDispatchesAndForwards() throws Exception {
        assertTrue(localContext.getBean(IRealtimeInboundService.class).hasListeners());
        assertTrue(remoteContext.getBean(IRealtimeInboundService.class).hasListeners());

        IRealtimeInboundReceiverService realtimeInboundReceiverService =
                localContext.getBean(IRealtimeInboundReceiverService.class);
        waitUntil(() -> realtimeInboundReceiverService.findAll().size() == 1, 10);
        assertEquals(1, realtimeInboundReceiverService.findAll().size());

        postRemoteInbound("remote-direct");
        assertEquals("remote:remote-direct", RemoteInboundListener.EVENTS.poll(5, TimeUnit.SECONDS));

        BlockingQueue<String> websocketMessages = new LinkedBlockingQueue<>();
        WebSocketSession session = connectWebSocketClient(websocketMessages);
        try {
            assertNotNull(websocketMessages.poll(5, TimeUnit.SECONDS));

            session.sendMessage(new TextMessage("""
                    {"id":"m1","version":"1.0","event":{"domain":"task","name":"cancel"},"payload":{"type":"text","text":"from-ws"}}
                    """));

            assertEquals("local-primary:from-ws", LocalPrimaryInboundListener.EVENTS.poll(5, TimeUnit.SECONDS));
            assertEquals("local-extra:from-ws", LocalExtraInboundListener.EVENTS.poll(5, TimeUnit.SECONDS));
            assertEquals("remote:from-ws", RemoteInboundListener.EVENTS.poll(5, TimeUnit.SECONDS));
        } finally {
            session.close();
        }
    }

    private void postRemoteInbound(String content) {
        RealtimeInboundMessage message = new RealtimeInboundMessage(
                "r1",
                "1.0",
                RealtimeEvent.of("task", "cancel"),
                new RealtimeSource("server", null, "s1"),
                RealtimeContext.of("tenant-a", 1001L),
                null,
                null,
                RealtimePayload.text(content),
                null,
                null,
                null,
                null);
        WebClient.create()
                .post()
                .uri("http://localhost:" + remotePort + "/_realtime/messages/inbound")
                .bodyValue(message)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(5));
    }

    private WebSocketSession connectWebSocketClient(BlockingQueue<String> receivedMessages) throws Exception {
        String url = "ws://localhost:" + localPort + "/realtime/transports/websocket?token=test-token&tenantId=tenant-a&userId=1001";
        StandardWebSocketClient client = new StandardWebSocketClient();
        return client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                receivedMessages.offer(message.getPayload());
            }
        }, url).get(5, TimeUnit.SECONDS);
    }

    private static void waitUntil(Check condition, int timeoutSeconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            if (condition.ok()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        throw new IllegalStateException("Condition not satisfied within " + timeoutSeconds + " seconds");
    }

    @FunctionalInterface
    private interface Check {
        boolean ok();
    }
}
