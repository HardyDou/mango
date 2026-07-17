package io.mango.ai.core.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DeepSeek HTTP 协议边界测试。
 */
class DeepSeekProviderTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void chat_标准流式响应_转换思考与消息事件() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            String body = "data: {\"choices\":[{\"delta\":{\"thinking\":\"plan\"}}]}\n\n"
                    + "data: {\"choices\":[{\"delta\":{\"content\":\"answer\"}}]}\n\n"
                    + "data: [DONE]\n\n";
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(body.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.start();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
        DeepSeekProvider provider = new DeepSeekProvider(
                new ObjectMapper(), webClient, "deepseek-chat", Duration.ofSeconds(2));

        List<String> events = provider.chat(
                        List.of(Map.of("role", "user", "content", "hello")), true)
                .collectList()
                .block();

        assertEquals(2, events.size());
        assertTrue(events.get(0).contains("\"type\":\"thinking\""));
        assertTrue(events.get(1).contains("\"type\":\"message\""));
    }

    @Test
    void chat_非法响应_转换为错误事件() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] body = "data: not-json\n\n".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        DeepSeekProvider provider = new DeepSeekProvider(
                new ObjectMapper(),
                WebClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build(),
                "deepseek-chat",
                Duration.ofSeconds(2));

        List<String> events = provider.chat(
                        List.of(Map.of("role", "user", "content", "hello")), false)
                .collectList()
                .block();

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("Failed to parse response"));
    }
}
