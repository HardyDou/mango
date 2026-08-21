package io.mango.ai.starter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通过真实嵌入式 HTTP 服务验证 AI starter 入口。
 */
@Tag("flow")
@Tag("ai")
@SpringBootTest(
        classes = AiHttpContractFlowTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mango.kv.store.type=redis",
                "spring.ai.deepseek.api-key=test-key",
                "spring.autoconfigure.exclude=org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration,"
                        + "io.mango.infra.kv.starter.KvStoreAutoConfiguration,"
                        + "io.mango.infra.kv.starter.redis.KvRedisAutoConfiguration,"
                        + "io.mango.infra.kv.starter.KvCapabilityAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceDataSourceAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceFlywayAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "io.mango.infra.realtime.starter.MangoRealtimeAutoConfiguration"
        })
class AiRuntimeFlowTest {

    @LocalServerPort
    private int port;

    @Test
    void chat_真实HTTP入口_返回标准Sse事件() {
        WebClient client = WebClient.builder().baseUrl("http://127.0.0.1:" + port).build();

        String body = client.post()
                .uri("/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue("{\"message\":\"hello\",\"sessionId\":\"runtime-session\",\"enableThinking\":false}")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));

        assertTrue(body.contains("data:"), body);
        assertTrue(body.contains("\"type\":\"message\""), body);
        assertTrue(body.contains("\"type\":\"done\""), body);
        assertTrue(body.contains("runtime-session"), body);
    }

}
