package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI 进程内推送行为测试。
 */
class AiPushServiceTest {

    @Test
    void connect_连接后可接收通知与告警广播() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AiPushService service = new AiPushService(objectMapper, 25_000L);
        CopyOnWriteArrayList<String> received = new CopyOnWriteArrayList<>();

        Disposable connection = service.connect().take(3).subscribe(received::add);
        service.broadcastNotification("notice");
        service.broadcastAlert("alert");

        assertEquals(3, received.size());
        assertEvent(objectMapper, received.get(0), "connected", "SSE connected");
        assertEvent(objectMapper, received.get(1), "notification", "notice");
        assertEvent(objectMapper, received.get(2), "alert", "alert");
        connection.dispose();
    }

    private void assertEvent(
            ObjectMapper objectMapper, String event, String type, String content) throws Exception {
        JsonNode json = objectMapper.readTree(event);
        assertEquals(type, json.path("type").asText());
        assertEquals(content, json.path("content").asText());
    }
}
