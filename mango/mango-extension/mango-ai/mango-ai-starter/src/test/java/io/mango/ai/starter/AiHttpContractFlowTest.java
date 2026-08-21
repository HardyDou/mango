package io.mango.ai.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.IRateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI HTTP 入口特征测试，验证 Spring AI 模型到 SSE 的真实适配契约。
 */
@Tag("flow")
@Tag("ai")
@SpringBootTest(classes = AiHttpContractFlowTest.TestApplication.class, properties = {
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
@AutoConfigureMockMvc
class AiHttpContractFlowTest {

    private final MockMvc mockMvc;

    @Autowired
    AiHttpContractFlowTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void chat_空消息_由BeanValidation拒绝() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_非法会话标识_由命令校验拒绝() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\",\"sessionId\":\"../../shared\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void legacyAiSseEndpoint_已移除() throws Exception {
        mockMvc.perform(get("/ai/sse"))
                .andExpect(status().isNotFound());
    }

    @Test
    void chat_合法请求_返回标准Sse消息与会话完成事件() throws Exception {
        MvcResult result = mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"hello\",\"sessionId\":\"session-1\",\"enableThinking\":false}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = responseBody(result);
        assertTrue(body.contains("\"type\":\"message\""), body);
        assertTrue(body.contains("\"type\":\"done\""), body);
        assertTrue(body.contains("session-1"), body);
        assertTrue(body.contains("data:"), body);
    }

    @Test
    void chat_enableThinking显式Null_使用命令默认值() throws Exception {
        MvcResult result = mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"hello\",\"enableThinking\":null}"))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(responseBody(result).contains("\"type\":\"message\""));
    }

    private String responseBody(MvcResult result) throws Exception {
        if (result.getRequest().isAsyncStarted()) {
            return mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
        }
        return result.getResponse().getContentAsString();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({MangoAiAutoConfiguration.class, TestContextConfiguration.class})
    static class TestApplication {

        @Bean
        @Primary
        ChatModel testChatModel() {
            return new TestChatModel();
        }

        @Bean
        ICache testCache() {
            return new TestCache();
        }

        @Bean
        IRateLimiter testRateLimiter() {
            return (key, permits) -> true;
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    static class TestContextConfiguration {

        @Bean
        org.springframework.boot.web.servlet.FilterRegistrationBean<TestContextFilter> testContextFilter() {
            org.springframework.boot.web.servlet.FilterRegistrationBean<TestContextFilter> registration =
                    new org.springframework.boot.web.servlet.FilterRegistrationBean<>();
            registration.setFilter(new TestContextFilter());
            registration.addUrlPatterns("/*");
            registration.setOrder(Integer.MIN_VALUE + 20);
            return registration;
        }
    }

    private static final class TestContextFilter extends org.springframework.web.filter.OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, java.io.IOException {
            MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                    101L, "tenant-test", "test-user", "tenant", "MEMBER", "USER", 101L, "admin"));
            try {
                filterChain.doFilter(request, response);
            } finally {
                MangoContextHolder.clear();
            }
        }
    }

    private static final class TestChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            return response();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(response());
        }

        private ChatResponse response() {
            return new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("answer"))));
        }
    }

    private static final class TestCache implements ICache {

        private final Map<String, String> values = new HashMap<>();

        @Override
        public void set(String key, String value, long ttlSeconds) {
            values.put(key, value);
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public boolean exists(String key) {
            return values.containsKey(key);
        }

        @Override
        public void delete(String key) {
            values.remove(key);
        }
    }
}
