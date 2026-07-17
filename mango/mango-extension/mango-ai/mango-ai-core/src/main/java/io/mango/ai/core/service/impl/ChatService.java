package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.api.dto.ChatRequest;
import io.mango.ai.api.enums.AiCode;
import io.mango.ai.core.provider.IAiProvider;
import io.mango.ai.core.service.IChatService;
import io.mango.common.result.Require;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存会话的 AI 对话服务。
 */
@Service
public class ChatService implements IChatService {

    private static final int MAX_HISTORY_SIZE = 20;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_SESSION_ID_LENGTH = 128;
    private static final Set<String> PROMPT_INJECTION_PATTERNS = Set.of(
            "ignore previous instructions",
            "ignore all previous",
            "disregard your instructions",
            "you are now",
            "pretend you are",
            "as an ai");

    private final IAiProvider aiProvider;
    private final ObjectMapper objectMapper;
    private final Map<String, ChatContext> sessionContexts = new ConcurrentHashMap<>();
    private final long sessionTtl;

    /**
     * 创建对话服务。
     *
     * @param aiProvider AI 模型端口
     * @param objectMapper JSON 序列化器
     * @param sessionTtl 会话有效期，毫秒
     */
    public ChatService(
            IAiProvider aiProvider,
            ObjectMapper objectMapper,
            @Value("${mango.ai.session.ttl:1800000}") long sessionTtl) {
        this.aiProvider = aiProvider;
        this.objectMapper = objectMapper.copy();
        this.sessionTtl = sessionTtl;
    }

    @Override
    public Flux<String> chat(ChatRequest request, String tenantId) {
        Require.notNull(request, AiCode.CHAT_REQUEST_REQUIRED, "对话请求不能为空");
        Require.notBlank(tenantId, AiCode.TENANT_REQUIRED, "租户标识不能为空");
        Require.notBlank(request.getMessage(), AiCode.CHAT_REQUEST_INVALID, "对话内容不能为空");
        cleanupExpiredSessions();

        String message = request.getMessage().trim();
        Require.isTrue(
                message.length() <= MAX_MESSAGE_LENGTH,
                AiCode.CHAT_REQUEST_INVALID,
                "对话内容长度不能超过2000个字符");
        Require.isTrue(
                request.getSessionId() == null
                        || request.getSessionId().length() <= MAX_SESSION_ID_LENGTH,
                AiCode.CHAT_REQUEST_INVALID,
                "会话ID长度不能超过128个字符");
        if (containsPromptInjection(message)) {
            return Flux.just(jsonEvent("error", "Invalid message format detected"));
        }
        String sessionId = resolveSessionId(request.getSessionId());
        boolean enableThinking = request.getEnableThinking() == null || request.getEnableThinking();
        List<Map<String, String>> messages = getConversationHistory(tenantId, sessionId);
        messages.add(Map.of("role", "user", "content", message));

        return aiProvider.chat(messages, enableThinking)
                .concatWithValues(jsonDoneEvent(sessionId))
                .doOnComplete(() -> saveToHistory(tenantId, sessionId, message))
                .onErrorReturn(jsonEvent("error", "AI service error"));
    }

    private List<Map<String, String>> getConversationHistory(String tenantId, String sessionId) {
        ChatContext context = sessionContexts.get(buildKey(tenantId, sessionId));
        if (context == null) {
            return new ArrayList<>();
        }
        return context.messagesCopy();
    }

    private void saveToHistory(String tenantId, String sessionId, String userMessage) {
        sessionContexts.computeIfAbsent(
                        buildKey(tenantId, sessionId), ignored -> new ChatContext())
                .add(Map.of("role", "user", "content", userMessage));
    }

    private void cleanupExpiredSessions() {
        long cutoff = System.currentTimeMillis() - sessionTtl;
        sessionContexts.entrySet().removeIf(entry -> entry.getValue().lastAccessTime() < cutoff);
    }

    private String buildKey(String tenantId, String sessionId) {
        return tenantId + '_' + sessionId;
    }

    private boolean containsPromptInjection(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return PROMPT_INJECTION_PATTERNS.stream().anyMatch(lower::contains);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveSessionId(String sessionId) {
        if (hasText(sessionId)) {
            return sessionId;
        }
        return UUID.randomUUID().toString();
    }

    private String jsonEvent(String type, String message) {
        return writeJson(Map.of("type", type, "message", message));
    }

    private String jsonDoneEvent(String sessionId) {
        return writeJson(Map.of("type", "done", "sessionId", sessionId));
    }

    private String writeJson(Map<String, String> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            return Require.rethrow(
                    new IllegalStateException("Failed to serialize AI stream event", exception));
        }
    }

    private static final class ChatContext {
        private List<Map<String, String>> messages = new ArrayList<>();
        private long lastAccessTime = System.currentTimeMillis();

        private synchronized List<Map<String, String>> messagesCopy() {
            lastAccessTime = System.currentTimeMillis();
            return new ArrayList<>(messages);
        }

        private synchronized void add(Map<String, String> message) {
            messages.add(message);
            if (messages.size() > MAX_HISTORY_SIZE) {
                messages = new ArrayList<>(messages.subList(messages.size() - MAX_HISTORY_SIZE, messages.size()));
            }
            lastAccessTime = System.currentTimeMillis();
        }

        private synchronized long lastAccessTime() {
            return lastAccessTime;
        }
    }
}
