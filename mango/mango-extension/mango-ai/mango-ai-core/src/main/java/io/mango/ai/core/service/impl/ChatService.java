package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.mango.ai.api.command.ChatCommand;
import io.mango.ai.api.enums.AiCode;
import io.mango.ai.core.service.IChatService;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.infra.log.Loggers;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 基于 Spring AI 与 Mango KV 的流式对话服务。
 */
@Service
public class ChatService implements IChatService {

    private static final Logger LOG = LoggerFactory.getLogger(ChatService.class);
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger(Loggers.OPERATION);
    private static final TypeReference<List<StoredMessage>> HISTORY_TYPE = new TypeReference<>() { };
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_ERROR = "error";
    private static final String RESULT_RATE_LIMITED = "rate_limited";
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_SESSION_ID_LENGTH = 128;
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");

    private final ChatModel chatModel;
    private final ICache cache;
    private final IRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker;
    private final long sessionTtlSeconds;
    private final int maxHistoryMessages;

    /**
     * 创建 AI 对话服务。
     *
     * @param chatModel Spring AI 模型
     * @param cache Mango 缓存能力
     * @param rateLimiter Mango 限流能力
     * @param objectMapper JSON 序列化器
     * @param meterRegistry 指标注册器
     * @param sessionTtl 会话上下文有效期
     * @param maxHistoryMessages 最大历史消息数
     * @param failureRateThreshold 熔断失败率阈值
     * @param minimumCalls 熔断统计最小调用数
     * @param openStateDuration 熔断打开时长
     */
    public ChatService(
            ChatModel chatModel,
            ICache cache,
            IRateLimiter rateLimiter,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${mango.ai.chat.session-ttl:30m}") Duration sessionTtl,
            @Value("${mango.ai.chat.max-history-messages:20}") int maxHistoryMessages,
            @Value("${mango.ai.chat.circuit-breaker.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${mango.ai.chat.circuit-breaker.minimum-calls:5}") int minimumCalls,
            @Value("${mango.ai.chat.circuit-breaker.open-state-duration:30s}") Duration openStateDuration) {
        this.chatModel = chatModel;
        this.cache = cache;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper.copy();
        this.meterRegistry = meterRegistry;
        this.sessionTtlSeconds = positiveSeconds(sessionTtl);
        Require.positive(maxHistoryMessages, "maxHistoryMessages must be positive");
        this.maxHistoryMessages = maxHistoryMessages;
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .minimumNumberOfCalls(minimumCalls)
                .slidingWindowSize(minimumCalls)
                .waitDurationInOpenState(openStateDuration)
                .build();
        this.circuitBreaker = CircuitBreaker.of("mangoAiChatModel", config);
    }

    @Override
    public Flux<String> chat(ChatCommand command) {
        Require.notNull(command, AiCode.CHAT_REQUEST_REQUIRED, "对话请求不能为空");
        Require.notBlank(command.getMessage(), AiCode.CHAT_REQUEST_INVALID, "对话内容不能为空");
        Require.notNull(command.getEnableThinking(), AiCode.CHAT_REQUEST_INVALID, "思考模式不能为空");
        String tenantId = MangoContextHolder.tenantId();
        Long userId = MangoContextHolder.userId();
        Require.notBlank(tenantId, AiCode.TENANT_REQUIRED, "租户标识不能为空");
        Require.notNull(userId, AiCode.USER_REQUIRED, "用户标识不能为空");

        String sessionId = resolveSessionId(command.getSessionId());
        String userMessage = command.getMessage().trim();
        Require.isTrue(
                userMessage.length() <= MAX_MESSAGE_LENGTH,
                AiCode.CHAT_REQUEST_INVALID,
                "对话内容长度不能超过2000个字符");
        boolean thinkingEnabled = Boolean.TRUE.equals(command.getEnableThinking());
        InvocationContext context = new InvocationContext(
                tenantId,
                userId,
                sessionId,
                userMessage,
                thinkingEnabled,
                MangoContextHolder.traceId(),
                System.nanoTime());
        return Flux.defer(() -> execute(context))
                .onErrorResume(error -> modelError(context, TokenUsage.empty(), error));
    }

    private Flux<String> execute(InvocationContext context) {
        if (!rateLimiter.tryAcquire(rateLimitKey(context), 1)) {
            recordInvocation(context, RESULT_RATE_LIMITED, null, "none", TokenUsage.empty());
            return Flux.just(errorEvent(AiCode.CHAT_RATE_LIMITED.getMessage()));
        }

        ConversationAccumulator accumulator = new ConversationAccumulator();
        List<StoredMessage> history = loadHistory(context);
        List<Message> promptMessages = toSpringMessages(history);
        promptMessages.add(new UserMessage(context.userMessage()));
        Prompt prompt = new Prompt(promptMessages);

        return chatModel.stream(prompt)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .concatMap(response -> mapResponse(response, context.thinkingEnabled(), accumulator))
                .concatWith(Mono.defer(() -> completeConversation(context, history, accumulator)))
                .onErrorResume(error -> modelError(context, accumulator.tokenUsage(), error));
    }

    private Flux<String> mapResponse(
            ChatResponse response, boolean thinkingEnabled, ConversationAccumulator accumulator) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return Flux.empty();
        }
        AssistantMessage output = response.getResult().getOutput();
        if (response.getMetadata() != null) {
            accumulator.captureUsage(response.getMetadata().getUsage());
            accumulator.captureModel(response.getMetadata().getModel());
        }
        List<String> events = new ArrayList<>(2);
        if (thinkingEnabled && output instanceof DeepSeekAssistantMessage deepSeekMessage) {
            String reasoning = deepSeekMessage.getReasoningContent();
            if (hasText(reasoning)) {
                events.add(contentEvent("thinking", reasoning));
            }
        }
        String content = output.getText();
        if (hasText(content)) {
            accumulator.append(content);
            events.add(contentEvent("message", content));
        }
        return Flux.fromIterable(events);
    }

    private Mono<String> completeConversation(
            InvocationContext context,
            List<StoredMessage> history,
            ConversationAccumulator accumulator) {
        return Mono.fromCallable(() -> {
            String assistantMessage = accumulator.content();
            Require.notBlank(
                    assistantMessage,
                    AiCode.CHAT_MODEL_UNAVAILABLE,
                    "AI 模型未返回有效内容");
            saveHistory(context, history, assistantMessage);
            recordInvocation(
                    context,
                    RESULT_SUCCESS,
                    null,
                    accumulator.model(),
                    accumulator.tokenUsage());
            return doneEvent(context.sessionId());
        });
    }

    private Flux<String> modelError(
            InvocationContext context, TokenUsage tokenUsage, Throwable error) {
        String errorType = error instanceof CallNotPermittedException
                ? "circuit_open"
                : error.getClass().getSimpleName();
        LOG.error(
                "AI chat invocation failed tenantId={} userId={} sessionId={} traceId={} errorType={}",
                context.tenantId(),
                context.userId(),
                context.sessionId(),
                context.traceId(),
                errorType,
                error);
        recordInvocation(context, RESULT_ERROR, errorType, "none", tokenUsage);
        return Flux.just(errorEvent(AiCode.CHAT_MODEL_UNAVAILABLE.getMessage()));
    }

    private List<StoredMessage> loadHistory(InvocationContext context) {
        String value = cache.get(conversationKey(context));
        if (!hasText(value)) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(value, HISTORY_TYPE));
        } catch (JsonProcessingException exception) {
            return Require.rethrow(new IllegalStateException(
                    AiCode.CHAT_CONTEXT_UNAVAILABLE.getMessage(), exception));
        }
    }

    private void saveHistory(
            InvocationContext context, List<StoredMessage> history, String assistantMessage) {
        history.add(new StoredMessage(ROLE_USER, context.userMessage()));
        history.add(new StoredMessage(ROLE_ASSISTANT, assistantMessage));
        List<StoredMessage> retained = history.size() <= maxHistoryMessages
                ? history
                : history.subList(history.size() - maxHistoryMessages, history.size());
        try {
            cache.set(
                    conversationKey(context),
                    objectMapper.writeValueAsString(retained),
                    sessionTtlSeconds);
        } catch (JsonProcessingException exception) {
            Require.rethrow(new IllegalStateException(
                    AiCode.CHAT_CONTEXT_UNAVAILABLE.getMessage(), exception));
        }
    }

    private List<Message> toSpringMessages(List<StoredMessage> history) {
        List<Message> messages = new ArrayList<>(history.size());
        for (StoredMessage message : history) {
            if (ROLE_USER.equals(message.role())) {
                messages.add(new UserMessage(message.content()));
            } else if (ROLE_ASSISTANT.equals(message.role())) {
                messages.add(new AssistantMessage(message.content()));
            }
        }
        return messages;
    }

    private void recordInvocation(
            InvocationContext context,
            String result,
            String errorType,
            String model,
            TokenUsage usage) {
        meterRegistry.counter("mango.ai.chat.requests", "result", result).increment();
        meterRegistry.counter("mango.ai.chat.tokens", "type", "prompt")
                .increment(usage.promptTokens());
        meterRegistry.counter("mango.ai.chat.tokens", "type", "completion")
                .increment(usage.completionTokens());
        long durationMillis = Duration.ofNanos(System.nanoTime() - context.startedNanos()).toMillis();
        AUDIT_LOG.info(
                "event=ai_chat tenantId={} userId={} sessionId={} traceId={} model={} result={} errorType={} "
                        + "promptTokens={} completionTokens={} durationMs={}",
                context.tenantId(),
                context.userId(),
                context.sessionId(),
                context.traceId(),
                model,
                result,
                errorType == null ? "none" : errorType,
                usage.promptTokens(),
                usage.completionTokens(),
                durationMillis);
    }

    private String conversationKey(InvocationContext context) {
        return "ai-chat:conversation:" + context.tenantId() + ':'
                + context.userId() + ':' + context.sessionId();
    }

    private String rateLimitKey(InvocationContext context) {
        return "ai-chat:user:" + context.tenantId() + ':' + context.userId();
    }

    private String resolveSessionId(String sessionId) {
        if (hasText(sessionId)) {
            String normalized = sessionId.trim();
            Require.isTrue(
                    normalized.length() <= MAX_SESSION_ID_LENGTH
                            && SESSION_ID_PATTERN.matcher(normalized).matches(),
                    AiCode.CHAT_REQUEST_INVALID,
                    "会话标识格式不正确");
            return normalized;
        }
        return UUID.randomUUID().toString();
    }

    private String contentEvent(String type, String content) {
        return writeJson(Map.of("type", type, "content", content));
    }

    private String errorEvent(String message) {
        return writeJson(Map.of("type", "error", "message", message));
    }

    private String doneEvent(String sessionId) {
        return writeJson(Map.of("type", "done", "sessionId", sessionId));
    }

    private String writeJson(Map<String, String> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            return Require.rethrow(new IllegalStateException(
                    "AI 流式事件序列化失败", exception));
        }
    }

    private static long positiveSeconds(Duration duration) {
        Require.notNull(duration, "sessionTtl cannot be null");
        long seconds = duration.toSeconds();
        Require.positive(seconds, "sessionTtl must be positive");
        return seconds;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record InvocationContext(
            String tenantId,
            Long userId,
            String sessionId,
            String userMessage,
            boolean thinkingEnabled,
            String traceId,
            long startedNanos) {
    }

    private record StoredMessage(String role, String content) {
    }

    private record TokenUsage(int promptTokens, int completionTokens) {

        private static TokenUsage empty() {
            return new TokenUsage(0, 0);
        }

        private static TokenUsage from(Usage usage) {
            if (usage == null) {
                return empty();
            }
            return new TokenUsage(valueOrZero(usage.getPromptTokens()), valueOrZero(usage.getCompletionTokens()));
        }

        private static int valueOrZero(Integer value) {
            return value == null ? 0 : value;
        }
    }

    private static final class ConversationAccumulator {

        private final StringBuilder content = new StringBuilder();
        private TokenUsage tokenUsage = TokenUsage.empty();
        private String model = "none";

        private void append(String delta) {
            content.append(delta);
        }

        private void captureUsage(Usage usage) {
            TokenUsage current = TokenUsage.from(usage);
            if (current.promptTokens() > 0 || current.completionTokens() > 0) {
                tokenUsage = current;
            }
        }

        private void captureModel(String model) {
            if (hasText(model)) {
                this.model = model;
            }
        }

        private String content() {
            return content.toString();
        }

        private TokenUsage tokenUsage() {
            return tokenUsage;
        }

        private String model() {
            return model;
        }
    }
}
