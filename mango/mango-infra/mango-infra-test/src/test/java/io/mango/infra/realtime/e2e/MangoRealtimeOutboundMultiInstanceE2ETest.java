package io.mango.infra.realtime.e2e;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.e2e.support.RealtimeTestApps;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MangoRealtimeOutboundMultiInstanceE2ETest {

    private static ConfigurableApplicationContext nodeAContext;
    private static ConfigurableApplicationContext nodeBContext;
    private static int nodeBPort;

    @BeforeAll
    static void setUpContexts() {
        RealtimeTestApps.StartedRealtimeNodes startedNodes = RealtimeTestApps.startTwoRealtimeNodes();
        nodeAContext = startedNodes.nodeAContext();
        nodeBContext = startedNodes.nodeBContext();
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
    void publishToUser_routesAcrossRealtimeInstancesAndDispatchesLocally() throws Exception {
        BlockingQueue<String> websocketMessages = new LinkedBlockingQueue<>();
        WebSocketSession session = connectWebSocketClient(websocketMessages, nodeBPort, 2001L);
        try {
            assertNotNull(websocketMessages.poll(5, TimeUnit.SECONDS));

            nodeAContext.getBean(RealtimeApi.class).publishToUser(2001L, "task.done", "from-node-a");

            String delivered = websocketMessages.poll(5, TimeUnit.SECONDS);
            assertNotNull(delivered);
            assertTrue(delivered.contains("task.done"));
            assertTrue(delivered.contains("from-node-a"));
        } finally {
            session.close();
        }
    }

    private WebSocketSession connectWebSocketClient(BlockingQueue<String> receivedMessages,
                                                   int port,
                                                   Long userId) throws Exception {
        String url = "ws://localhost:" + port + "/realtime/transports/websocket?tenantId=tenant-a&userId=" + userId;
        StandardWebSocketClient client = new StandardWebSocketClient();
        return client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                receivedMessages.offer(message.getPayload());
            }
        }, url).get(5, TimeUnit.SECONDS);
    }
}
