package io.mango.infra.realtime.integration;

import io.mango.infra.realtime.api.RealtimeApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency and mixed-protocol integration tests for mango-infra-realtime.
 * These tests verify behavior under concurrent load and multi-protocol scenarios
 * that the basic smoke tests do not cover.
 */
@SpringBootTest(
        classes = MangoRealtimeConcurrencyIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude="
                        + "io.mango.infra.redis.starter.RedisAutoConfiguration,"
                        + "io.mango.infra.kv.starter.KvStoreAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceFlywayAutoConfiguration"
        })
class MangoRealtimeConcurrencyIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RealtimeApi realtimeApi;

    // ===== Concurrent message send =====

    @Test
    void concurrent_publishToUser_noExceptions() throws Exception {
        int senderCount = 10;
        int messagesPerSender = 20;
        BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
        WebSocketSession session = connectWebSocketClient(
                receivedMessages, "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=concurrent&userId=9999"
        );

        try {
            assertNotNull(receivedMessages.poll(5, TimeUnit.SECONDS));

            ExecutorService executor = Executors.newFixedThreadPool(senderCount);
            CountDownLatch publishLatch = new CountDownLatch(senderCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicReference<Throwable> error = new AtomicReference<>();

            for (int s = 0; s < senderCount; s++) {
                final int senderId = s;
                executor.submit(() -> {
                    try {
                        for (int m = 0; m < messagesPerSender; m++) {
                            realtimeApi.publishToUser(9999L, "concurrent-msg", "sender-" + senderId + "-msg-" + m);
                            successCount.incrementAndGet();
                        }
                    } catch (Throwable t) {
                        error.set(t);
                    } finally {
                        publishLatch.countDown();
                    }
                });
            }

            publishLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            // Concurrent publish must not throw any exceptions
            assertNull(error.get(), "Concurrent publish threw: " + error.get());

            // All publish calls must succeed (basic counter check)
            assertEquals(senderCount * messagesPerSender, successCount.get());

            List<String> allMessages = new ArrayList<>();
            int expectedMessages = senderCount * messagesPerSender;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (allMessages.size() < expectedMessages && System.nanoTime() < deadline) {
                String msg = receivedMessages.poll(500, TimeUnit.MILLISECONDS);
                if (msg != null && msg.contains("concurrent-msg")) {
                    allMessages.add(msg);
                }
            }
            assertEquals(expectedMessages, allMessages.size(), "All concurrently published messages should be delivered");
        } finally {
            session.close();
        }
    }

    @Test
    void concurrent_subscribeAndUnsubscribe_noDuplicateDelivery() throws Exception {
        List<WebSocketSession> sessions = new ArrayList<>();
        List<BlockingQueue<String>> queues = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int userId = 8000 + i;
            BlockingQueue<String> queue = new LinkedBlockingQueue<>();
            WebSocketSession s = connectWebSocketClient(
                    queue,
                    "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=rapid&userId=" + userId
            );
            sessions.add(s);
            queues.add(queue);
            assertNotNull(queue.poll(5, TimeUnit.SECONDS));
        }

        for (int i = 0; i < 5; i++) {
            realtimeApi.publishToUser(8000L + i, "rapid-msg", "msg-for-" + (8000 + i));
        }

        for (int i = 0; i < 5; i++) {
            String message = queues.get(i).poll(5, TimeUnit.SECONDS);
            assertNotNull(message, "user " + (8000 + i) + " should receive one message");
            assertTrue(message.contains("msg-for-" + (8000 + i)));
            assertNull(queues.get(i).poll(300, TimeUnit.MILLISECONDS), "user " + (8000 + i) + " should not receive duplicates");
        }

        for (WebSocketSession s : sessions) {
            s.close();
        }
    }

    // ===== Mixed protocol (WS + SSE same userId) =====

    @Test
    void mixedProtocol_sameUserId_bothConnectionsReceiveMessage() throws Exception {
        BlockingQueue<String> wsReceived = new LinkedBlockingQueue<>();
        BlockingQueue<String> sseReceived = new LinkedBlockingQueue<>();

        // Both WS and SSE connect for userId 7001
        WebSocketSession wsSession = connectWebSocketClient(
                wsReceived,
                "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=mixed&userId=7001"
        );

        var sseSubscription = WebClient.create()
                .get()
                .uri("http://localhost:" + port + "/realtime/transports/sse?userId=7001")
                .header("Authorization", "Bearer test-token")
                .header("TENANT-ID", "mixed")
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .map(ServerSentEvent::data)
                .filter(data -> data != null && !data.isBlank())
                .subscribe(sseReceived::offer);

        try {
            // Wait for both connected
            assertNotNull(wsReceived.poll(5, TimeUnit.SECONDS));
            assertNotNull(sseReceived.poll(5, TimeUnit.SECONDS));

            // Publish to userId 7001
            realtimeApi.publishToUser(7001L, "mixed-msg", "dual-delivery");

            String wsMsg = wsReceived.poll(5, TimeUnit.SECONDS);
            String sseMsg = sseReceived.poll(5, TimeUnit.SECONDS);

            assertNotNull(wsMsg, "WS should have received the message");
            assertNotNull(sseMsg, "SSE should have received the message");
            assertTrue(wsMsg.contains("dual-delivery"));
            assertTrue(sseMsg.contains("dual-delivery"));
        } finally {
            sseSubscription.dispose();
            wsSession.close();
        }
    }

    @Test
    void mixedProtocol_userIdOnlyWSOnline_sseOffline_messageRoutedCorrectly() throws Exception {
        BlockingQueue<String> wsReceived = new LinkedBlockingQueue<>();

        // Only WS connects
        WebSocketSession wsSession = connectWebSocketClient(
                wsReceived,
                "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=mixed2&userId=7002"
        );

        try {
            assertNotNull(wsReceived.poll(5, TimeUnit.SECONDS));

            // Publish - WS should receive, SSE (not connected) obviously doesn't
            realtimeApi.publishToUser(7002L, "ws-only-msg", "ws-should-receive");

            String wsMsg = wsReceived.poll(5, TimeUnit.SECONDS);
            assertNotNull(wsMsg, "WS should receive even when SSE is not connected");
            assertTrue(wsMsg.contains("ws-should-receive"));
        } finally {
            wsSession.close();
        }
    }

    // ===== Session lifecycle =====

    @Test
    void lifecycle_connectDisconnectReconnect_resumesDelivery() throws Exception {
        BlockingQueue<String> received = new LinkedBlockingQueue<>();

        // First connection
        WebSocketSession session1 = connectWebSocketClient(
                received,
                "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=life&userId=6001"
        );
        assertNotNull(received.poll(5, TimeUnit.SECONDS)); // connected

        // Disconnect
        session1.close(CloseStatus.NORMAL);
        Thread.sleep(200);

        // Reconnect with same userId
        WebSocketSession session2 = connectWebSocketClient(
                received,
                "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=life&userId=6001"
        );
        assertNotNull(received.poll(5, TimeUnit.SECONDS)); // connected (session2)

        // Send message after reconnect
        realtimeApi.publishToUser(6001L, "after-reconnect", "message-after-reconnect");

        String msg = received.poll(5, TimeUnit.SECONDS);
        assertNotNull(msg, "Message should arrive after reconnect");
        assertTrue(msg.contains("message-after-reconnect"));

        session2.close();
    }

    // ===== Broadcast =====

    @Test
    void broadcast_allConnectedSessionsReceive() throws Exception {
        BlockingQueue<String> received1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> received2 = new LinkedBlockingQueue<>();

        WebSocketSession session1 = connectWebSocketClient(
                received1,
                "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=bcast&userId=5001"
        );
        WebSocketSession session2 = connectWebSocketClient(
                received2,
                "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=bcast&userId=5002"
        );

        try {
            assertNotNull(received1.poll(5, TimeUnit.SECONDS));
            assertNotNull(received2.poll(5, TimeUnit.SECONDS));

            // Broadcast to all
            realtimeApi.broadcast("bcast", "hello everyone");

            String msg1 = received1.poll(5, TimeUnit.SECONDS);
            String msg2 = received2.poll(5, TimeUnit.SECONDS);

            assertNotNull(msg1);
            assertNotNull(msg2);
            assertTrue(msg1.contains("hello everyone"));
            assertTrue(msg2.contains("hello everyone"));
        } finally {
            session1.close();
            session2.close();
        }
    }

    // ===== Tenant isolation =====

    @Test
    void tenant_isolation_tenantAMessagesNotReceivedByTenantB() throws Exception {
        BlockingQueue<String> receivedA = new LinkedBlockingQueue<>();
        BlockingQueue<String> receivedB = new LinkedBlockingQueue<>();

        WebSocketSession sessionA = connectWebSocketClient(
                receivedA,
                "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=tenant-A&userId=4001"
        );
        WebSocketSession sessionB = connectWebSocketClient(
                receivedB,
                "ws://localhost:" + port + "/realtime/transports/websocket?token=test&tenantId=tenant-B&userId=4002"
        );

        try {
            assertNotNull(receivedA.poll(5, TimeUnit.SECONDS));
            assertNotNull(receivedB.poll(5, TimeUnit.SECONDS));

            // Publish to tenant-A only
            realtimeApi.publishToTenant("tenant-A", "tenant-msg", "secret-for-A");

            String msgA = receivedA.poll(5, TimeUnit.SECONDS);
            String msgB = receivedB.poll(2, TimeUnit.SECONDS); // short timeout - should NOT arrive

            assertNotNull(msgA, "Tenant-A should receive its tenant message");
            assertTrue(msgA.contains("secret-for-A"));
            assertNull(msgB, "Tenant-B should NOT receive Tenant-A's message");
        } finally {
            sessionA.close();
            sessionB.close();
        }
    }

    // ===== Helper =====

    private WebSocketSession connectWebSocketClient(BlockingQueue<String> queue, String url) throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        return client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                queue.offer(message.getPayload());
            }
        }, url).get(5, TimeUnit.SECONDS);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
