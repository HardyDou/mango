package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.api.command.ChatCommand;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.IRateLimiter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 对话业务规则测试。
 */
class ChatServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void chat_连续对话_向SpringAi传递完整用户与助手历史() {
        RecordingChatModel model = new RecordingChatModel(List.of("first-answer", "second-answer"));
        RecordingCache cache = new RecordingCache();
        ChatService service = service(model, cache, (key, permits) -> true, new SimpleMeterRegistry(), 5);
        setContext("tenant-a", 101L);

        service.chat(command("first", "session-1")).collectList().block();
        List<String> secondEvents = service.chat(command("second", "session-1")).collectList().block();

        assertEquals(
                List.of("USER:first", "ASSISTANT:first-answer", "USER:second"),
                model.invocations().get(1));
        assertTrue(secondEvents.stream().anyMatch(event -> event.contains("second-answer")));
        assertTrue(secondEvents.stream().anyMatch(event -> event.contains("\"type\":\"done\"")));
        assertTrue(cache.values.values().iterator().next().contains("first-answer"));
        assertTrue(cache.values.values().iterator().next().contains("second-answer"));
        assertEquals(1800L, cache.lastTtlSeconds);
    }

    @Test
    void chat_相同会话标识_按租户和用户隔离上下文() {
        RecordingChatModel model = new RecordingChatModel(List.of("a", "b", "c"));
        RecordingCache cache = new RecordingCache();
        ChatService service = service(model, cache, (key, permits) -> true, new SimpleMeterRegistry(), 20);

        setContext("tenant-a", 101L);
        service.chat(command("tenant-a-user-101", "shared")).collectList().block();
        setContext("tenant-a", 102L);
        service.chat(command("tenant-a-user-102", "shared")).collectList().block();
        setContext("tenant-b", 101L);
        service.chat(command("tenant-b-user-101", "shared")).collectList().block();

        assertEquals(List.of("USER:tenant-a-user-102"), model.invocations().get(1));
        assertEquals(List.of("USER:tenant-b-user-101"), model.invocations().get(2));
        assertEquals(3, cache.values.size());
    }

    @Test
    void chat_超过历史上限_保留最近完整消息() throws Exception {
        RecordingChatModel model = new RecordingChatModel(List.of("a1", "a2", "a3"));
        RecordingCache cache = new RecordingCache();
        ChatService service = service(model, cache, (key, permits) -> true, new SimpleMeterRegistry(), 4);
        setContext("tenant-a", 101L);

        service.chat(command("u1", "session-1")).collectList().block();
        service.chat(command("u2", "session-1")).collectList().block();
        service.chat(command("u3", "session-1")).collectList().block();

        List<Map<String, String>> stored = OBJECT_MAPPER.readValue(
                cache.values.values().iterator().next(),
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Map.class));
        assertEquals(4, stored.size());
        assertEquals(List.of("u2", "a2", "u3", "a3"),
                stored.stream().map(message -> message.get("content")).toList());
    }

    @Test
    void chat_限流拒绝_不调用模型也不写会话() {
        RecordingChatModel model = new RecordingChatModel(List.of("unused"));
        RecordingCache cache = new RecordingCache();
        ChatService service = service(model, cache, (key, permits) -> false, new SimpleMeterRegistry(), 20);
        setContext("tenant-a", 101L);

        List<String> events = service.chat(command("hello", "session-1")).collectList().block();

        assertTrue(model.invocations().isEmpty());
        assertTrue(cache.values.isEmpty());
        assertTrue(events.getFirst().contains("AI 对话请求过于频繁"));
    }

    @Test
    void chat_模型持续失败_熔断打开后不再调用模型() {
        FailingChatModel model = new FailingChatModel();
        ChatService service = service(
                model,
                new RecordingCache(),
                (key, permits) -> true,
                new SimpleMeterRegistry(),
                20);
        setContext("tenant-a", 101L);

        service.chat(command("one", "session-1")).collectList().block();
        service.chat(command("two", "session-2")).collectList().block();
        List<String> third = service.chat(command("three", "session-3")).collectList().block();

        assertEquals(2, model.invocations.get());
        assertTrue(third.getFirst().contains("AI 模型当前不可用"));
    }

    @Test
    void chat_成功调用_记录请求与Token指标() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatService service = service(
                new RecordingChatModel(List.of("answer")),
                new RecordingCache(),
                (key, permits) -> true,
                registry,
                20);
        setContext("tenant-a", 101L);

        service.chat(command("hello", "session-1")).collectList().block();

        assertEquals(1.0, registry.get("mango.ai.chat.requests").tag("result", "success").counter().count());
        assertEquals(7.0, registry.get("mango.ai.chat.tokens").tag("type", "prompt").counter().count());
        assertEquals(3.0, registry.get("mango.ai.chat.tokens").tag("type", "completion").counter().count());
    }

    @Test
    void chat_缺少租户或用户上下文_失败闭合() {
        ChatService service = service(
                new RecordingChatModel(List.of("unused")),
                new RecordingCache(),
                (key, permits) -> true,
                new SimpleMeterRegistry(),
                20);

        assertThrows(BizException.class, () -> service.chat(command("hello", "session-1")));
        setContext("tenant-a", null);
        assertThrows(BizException.class, () -> service.chat(command("hello", "session-1")));
    }

    @Test
    void chat_非法会话标识_在访问KV前拒绝() {
        RecordingCache cache = new RecordingCache();
        ChatService service = service(
                new RecordingChatModel(List.of("unused")),
                cache,
                (key, permits) -> true,
                new SimpleMeterRegistry(),
                20);
        setContext("tenant-a", 101L);

        assertThrows(BizException.class, () -> service.chat(command("hello", "../../shared")));
        assertTrue(cache.values.isEmpty());
    }

    private ChatService service(
            ChatModel model,
            ICache cache,
            IRateLimiter rateLimiter,
            SimpleMeterRegistry registry,
            int maxHistoryMessages) {
        return new ChatService(
                model,
                cache,
                rateLimiter,
                OBJECT_MAPPER,
                registry,
                Duration.ofMinutes(30),
                maxHistoryMessages,
                50.0F,
                2,
                Duration.ofSeconds(30));
    }

    private ChatCommand command(String message, String sessionId) {
        ChatCommand command = new ChatCommand();
        command.setMessage(message);
        command.setSessionId(sessionId);
        return command;
    }

    private void setContext(String tenantId, Long userId) {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                userId,
                tenantId,
                userId == null ? null : "user-" + userId,
                "tenant",
                "MEMBER",
                "USER",
                userId,
                "admin"));
    }

    private static ChatResponse response(String content) {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("deepseek-test")
                .usage(new DefaultUsage(7, 3, 10))
                .build();
        return new ChatResponse(
                List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage(content))),
                metadata);
    }

    private static final class RecordingChatModel implements ChatModel {

        private final List<String> responses;
        private final List<List<String>> invocations = new ArrayList<>();
        private final AtomicInteger index = new AtomicInteger();

        private RecordingChatModel(List<String> responses) {
            this.responses = responses;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return response(responses.get(index.getAndIncrement()));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            invocations.add(prompt.getInstructions().stream()
                    .map(this::description)
                    .toList());
            return Flux.just(response(responses.get(index.getAndIncrement())));
        }

        private String description(Message message) {
            return message.getMessageType().name() + ':' + message.getText();
        }

        private List<List<String>> invocations() {
            return invocations;
        }
    }

    private static final class FailingChatModel implements ChatModel {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new IllegalStateException("offline");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.defer(() -> {
                invocations.incrementAndGet();
                return Flux.error(new IllegalStateException("offline"));
            });
        }
    }

    private static final class RecordingCache implements ICache {

        private final Map<String, String> values = new HashMap<>();
        private long lastTtlSeconds;

        @Override
        public void set(String key, String value, long ttlSeconds) {
            values.put(key, value);
            lastTtlSeconds = ttlSeconds;
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
