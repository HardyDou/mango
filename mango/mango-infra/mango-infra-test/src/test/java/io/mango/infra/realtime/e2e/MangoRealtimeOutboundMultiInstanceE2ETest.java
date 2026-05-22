package io.mango.infra.realtime.e2e;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.e2e.support.RealtimeTestApps;
import io.mango.infra.realtime.core.presence.IRealtimePresenceService;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MangoRealtimeOutboundMultiInstanceE2ETest {

    private static ConfigurableApplicationContext nodeAContext;
    private static ConfigurableApplicationContext nodeBContext;
    private static int nodeAPort;
    private static int nodeBPort;

    @BeforeAll
    static void setUpContexts() {
        RealtimeTestApps.StartedRealtimeNodes startedNodes = RealtimeTestApps.startTwoRealtimeNodes();
        nodeAContext = startedNodes.nodeAContext();
        nodeBContext = startedNodes.nodeBContext();
        nodeAPort = startedNodes.nodeAPort();
        nodeBPort = startedNodes.nodeBPort();
    }

    @AfterAll
    static void tearDownContexts() {
        if (nodeBContext != null) {
            nodeBContext.close();
        }
        if (nodeAContext != null) {
            nodeAContext.close();
        }
    }

    @Test
    void groupMessage_fansOutToRemoteGroupMembersAcrossRealtimeInstances() throws Exception {
        BlockingQueue<String> nodeAMessages = new LinkedBlockingQueue<>();
        BlockingQueue<String> nodeBMessages = new LinkedBlockingQueue<>();
        WebSocketSession nodeASession = connectWebSocketClient(nodeAMessages, nodeAPort, 3001L, "client-a");
        WebSocketSession nodeBSession = connectWebSocketClient(nodeBMessages, nodeBPort, 3002L, "client-b");
        try {
            assertTrue(waitForMessage(nodeAMessages, "connection.connected", 5));
            assertTrue(waitForMessage(nodeBMessages, "connection.connected", 5));

            nodeASession.sendMessage(new TextMessage(groupSubscribeMessage("room-e2e", "client-a", 3001L)));
            nodeBSession.sendMessage(new TextMessage(groupSubscribeMessage("room-e2e", "client-b", 3002L)));
            assertTrue(waitForMessage(nodeAMessages, "\"name\":\"message.accepted\"", "room-e2e", 5));
            assertTrue(waitForMessage(nodeBMessages, "\"name\":\"message.accepted\"", "room-e2e", 5));
            assertTrue(waitForGroupPresence(nodeAContext.getBean(IRealtimePresenceService.class), "tenant-a", "room-e2e", 2, 5));

            nodeASession.sendMessage(new TextMessage(groupChatMessage("room-e2e", "client-a", 3001L, "hello remote group")));

            assertTrue(
                    waitForMessage(nodeBMessages, "\"domain\":\"chat\"", "\"name\":\"message.send\"", "hello remote group", 5),
                    () -> "expected remote group fanout, nodeBMessages=" + nodeBMessages);
            assertFalse(
                    waitForMessage(nodeAMessages, "\"domain\":\"chat\"", "\"name\":\"message.send\"", "hello remote group", 1),
                    () -> "source connection should not receive its own group fanout, nodeAMessages=" + nodeAMessages);
        } finally {
            nodeBSession.close();
            nodeASession.close();
        }
    }

    @Test
    void publishToUser_routesAcrossRealtimeInstancesAndDispatchesLocally() throws Exception {
        BlockingQueue<String> websocketMessages = new LinkedBlockingQueue<>();
        WebSocketSession session = connectWebSocketClient(websocketMessages, nodeBPort, 2001L, "client-user");
        try {
            assertTrue(waitForMessage(websocketMessages, "connection.connected", 5));
            assertTrue(waitForPresence(nodeAContext.getBean(IRealtimePresenceService.class), 2001L, 5));

            nodeBContext.getBean(RealtimeApi.class).publishToUser(2001L, "task.local", "from-node-b");
            assertTrue(
                    waitForMessage(websocketMessages, "\"domain\":\"task\"", "\"name\":\"local\"", "from-node-b", 5),
                    () -> "expected local task.local/from-node-b, localSessions="
                            + nodeBContext.getBean(RealtimeSubscriptionManager.class).findByUser(2001L)
                            + ", remainingMessages=" + websocketMessages);

            nodeAContext.getBean(RealtimeApi.class).publishToUser(2001L, "task.done", "from-node-a");

            assertTrue(
                    waitForMessage(websocketMessages, "\"domain\":\"task\"", "\"name\":\"done\"", "from-node-a", 5),
                    () -> "expected task.done/from-node-a, presences="
                            + nodeAContext.getBean(IRealtimePresenceService.class).findByUser(2001L)
                            + ", remainingMessages=" + websocketMessages);
        } finally {
            session.close();
        }
    }

    private WebSocketSession connectWebSocketClient(BlockingQueue<String> receivedMessages,
                                                   int port,
                                                   Long userId,
                                                   String clientId) throws Exception {
        String url = "ws://localhost:" + port + "/realtime/transports/websocket?tenantId=tenant-a&userId=" + userId
                + "&clientId=" + clientId;
        StandardWebSocketClient client = new StandardWebSocketClient();
        return client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                receivedMessages.offer(message.getPayload());
            }
        }, url).get(5, TimeUnit.SECONDS);
    }

    private String groupSubscribeMessage(String roomId, String clientId, Long userId) {
        return """
                {
                  "version":"1.0",
                  "event":{"domain":"system","name":"subscription.subscribe"},
                  "source":{"platform":"web","clientId":"%s"},
                  "context":{"tenantId":"tenant-a","userId":%d},
                  "target":{"type":"GROUP","id":"%s"},
                  "metadata":{"roomId":"%s"},
                  "payload":{"type":"text","text":"%s"}
                }
                """.formatted(clientId, userId, roomId, roomId, roomId);
    }

    private String groupChatMessage(String roomId, String clientId, Long userId, String text) {
        return """
                {
                  "version":"1.0",
                  "event":{"domain":"chat","name":"message.send"},
                  "source":{"platform":"web","clientId":"%s"},
                  "context":{"tenantId":"tenant-a","userId":%d},
                  "target":{"type":"GROUP","id":"%s"},
                  "metadata":{"roomId":"%s","senderName":"User %d","senderClientId":"%s"},
                  "payload":{"type":"text","text":"%s"}
                }
                """.formatted(clientId, userId, roomId, roomId, userId, clientId, text);
    }

    private boolean waitForMessage(BlockingQueue<String> messages, String expected, int timeoutSeconds) throws InterruptedException {
        return waitForMessage(messages, expected, null, timeoutSeconds);
    }

    private boolean waitForMessage(BlockingQueue<String> messages,
                                   String expected,
                                   String secondExpected,
                                   int timeoutSeconds) throws InterruptedException {
        return waitForMessage(messages, expected, secondExpected, null, timeoutSeconds);
    }

    private boolean waitForMessage(BlockingQueue<String> messages,
                                   String expected,
                                   String secondExpected,
                                   String thirdExpected,
                                   int timeoutSeconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            String message = messages.poll(Math.max(1, remainingMillis), TimeUnit.MILLISECONDS);
            if (message != null
                    && message.contains(expected)
                    && (secondExpected == null || message.contains(secondExpected))
                    && (thirdExpected == null || message.contains(thirdExpected))) {
                return true;
            }
        }
        return false;
    }

    private boolean waitForPresence(IRealtimePresenceService presenceService,
                                    Long userId,
                                    int timeoutSeconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            if (!presenceService.findByUser(userId).isEmpty()) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private boolean waitForGroupPresence(IRealtimePresenceService presenceService,
                                         String tenantId,
                                         String groupId,
                                         int expectedCount,
                                         int timeoutSeconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            if (presenceService.findByGroup(tenantId, groupId).size() >= expectedCount) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }
}
