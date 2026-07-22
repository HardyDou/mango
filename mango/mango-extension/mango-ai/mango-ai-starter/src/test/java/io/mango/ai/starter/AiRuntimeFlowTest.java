package io.mango.ai.starter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通过真实嵌入式 HTTP 服务验证 AI starter 入口。
 */
@Tag("flow")
@Tag("ai")
@SpringBootTest(
        classes = AiHttpContractFlowTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiRuntimeFlowTest {

    @LocalServerPort
    private int port;

    @Test
    void chat_真实HTTP入口_返回可解析的标准Sse事件() {
        WebClient client = WebClient.builder().baseUrl("http://127.0.0.1:" + port).build();

        List<String> events = client.post()
                .uri("/ai/chat")
                .header("Authorization", "Bearer test-token")
                .header("TENANT-ID", "tenant-runtime")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(Map.of(
                        "message", "hello",
                        "sessionId", "runtime-session",
                        "enableThinking", true))
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .block(Duration.ofSeconds(5));

        assertEquals(3, events.size());
        assertTrue(events.get(0).contains("\"type\":\"thinking\""));
        assertTrue(events.get(1).contains("\"type\":\"message\""));
        assertTrue(events.get(2).contains("runtime-session"));
        assertFalse(events.stream().anyMatch(event -> event.startsWith("data:")));
    }

}
