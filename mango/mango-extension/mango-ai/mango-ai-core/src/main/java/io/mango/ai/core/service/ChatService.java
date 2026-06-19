package io.mango.ai.core.service;

import io.mango.ai.api.dto.ChatRequest;
import io.mango.ai.core.provider.DeepSeekProvider;
import io.mango.infra.context.support.TtlExecutorDecorator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Chat service with in-memory session storage
 * <p>
 * P2 implementation uses ConcurrentHashMap + TTL for session management.
 * Sessions are automatically cleaned up after TTL expires.
 *
 * @author Mango
 */
@Slf4j
@Service
public class ChatService {

    private final DeepSeekProvider deepSeekProvider;
    private final TtlExecutorDecorator ttlExecutorDecorator;

    /**
     * Session TTL in milliseconds (default 30 minutes)
     */
    @Value("${mango.ai.session.ttl:1800000}")
    private long sessionTtl;

    /**
     * Maximum message length
     */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * Tenant to session contexts mapping
     * Key: tenantId_sessionId
     */
    private final Map<String, ChatContext> sessionContexts = new ConcurrentHashMap<>();

    /**
     * Scheduled executor for session cleanup
     */
    private ScheduledExecutorService cleanupExecutor;

    /**
     * Executor for async chat processing (TTL-decorated)
     */
    private ExecutorService chatExecutor;

    /**
     * SSE timeout in milliseconds
     */
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    /**
     * Shutdown await timeout in seconds
     */
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    @Autowired
    public ChatService(DeepSeekProvider deepSeekProvider, TtlExecutorDecorator ttlExecutorDecorator) {
        this.deepSeekProvider = deepSeekProvider;
        this.ttlExecutorDecorator = ttlExecutorDecorator;
    }

    @PostConstruct
    public void init() {
        this.cleanupExecutor = ttlExecutorDecorator.decorate(
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "chat-session-cleanup");
                t.setDaemon(true);
                return t;
            })
        );
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredSessions, 1, 1, TimeUnit.MINUTES);
        this.chatExecutor = ttlExecutorDecorator.decorate(
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "chat-async");
                t.setDaemon(true);
                return t;
            })
        );
    }

    @PreDestroy
    public void shutdown() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("ChatService cleanupExecutor shut down");
        }
        if (chatExecutor != null) {
            chatExecutor.shutdown();
            try {
                if (!chatExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    chatExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                chatExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("ChatService chatExecutor shut down");
        }
    }

    /**
     * Process chat request with SSE streaming response
     *
     * @param request chat request
     * @param tenantId tenant identifier
     * @return SSE emitter
     */
    public SseEmitter chat(ChatRequest request, String tenantId) {
        // Validate message
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return createErrorEmitter("Message is required");
        }

        String message = request.getMessage().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return createErrorEmitter("Message exceeds maximum length of " + MAX_MESSAGE_LENGTH + " characters");
        }

        // Security check: prompt injection detection
        if (deepSeekProvider.containsPromptInjection(message)) {
            log.warn("Prompt injection detected from tenant: {}", tenantId);
            return createErrorEmitter("Invalid message format detected");
        }

        // Default enableThinking to true if not specified
        final boolean enableThinkingFinal = request.getEnableThinking() == null || request.getEnableThinking();

        // Get or create session ID
        final String sessionIdFinal;
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionIdFinal = UUID.randomUUID().toString();
        } else {
            sessionIdFinal = sessionId;
        }

        // Create SSE emitter with timeout and error callbacks
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().data("data: {\"type\":\"error\",\"message\":\"Request timeout\"}"));
                emitter.complete();
            } catch (IOException e) {
                log.warn("Failed to send timeout event", e);
            }
        });
        emitter.onError(e -> {
            log.warn("SSE error for tenant: {}", tenantId, e);
            try {
                emitter.send(SseEmitter.event().data("data: {\"type\":\"error\",\"message\":\"Connection error\"}"));
                emitter.complete();
            } catch (IOException ex) {
                log.warn("Failed to send error event", ex);
            }
        });

        // Process chat in async thread (TTL-decorated executor preserves tenant context)
        CompletableFuture.runAsync(() -> {
            try {
                // Get conversation history
                List<Map<String, String>> messages = getConversationHistory(tenantId, sessionIdFinal);

                // Add user message
                messages.add(Map.of("role", "user", "content", message));

                // Call DeepSeek API with streaming
                Flux<String> stream = deepSeekProvider.chat(messages, sessionIdFinal, enableThinkingFinal);

                // Send events to SSE
                stream.doOnNext(event -> {
                    if (event != null) {
                        try {
                            emitter.send(SseEmitter.event().data(event));
                        } catch (IOException e) {
                            log.warn("Failed to send SSE event", e);
                        }
                    }
                }).doOnComplete(() -> {
                    // Send done event
                    try {
                        emitter.send(SseEmitter.event().data("data: {\"type\":\"done\",\"sessionId\":\"" + sessionIdFinal + "\"}"));
                        emitter.complete();
                    } catch (IOException e) {
                        log.warn("Failed to send completion event", e);
                    }

                    // Save assistant response to history
                    saveToHistory(tenantId, sessionIdFinal, message, enableThinkingFinal);
                }).doOnError(e -> {
                    log.error("Chat stream error for tenant: {}", tenantId, e);
                    try {
                        emitter.send(SseEmitter.event().data("data: {\"type\":\"error\",\"message\":\"AI service error\"}"));
                        emitter.completeWithError(e);
                    } catch (IOException ex) {
                        log.warn("Failed to send error event", ex);
                    }
                }).subscribe();

            } catch (Exception e) {
                log.error("Chat error for tenant: {}", tenantId, e);
                try {
                    emitter.send(SseEmitter.event().data("data: {\"type\":\"error\",\"message\":\"AI service error\"}"));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.warn("Failed to send error event", ex);
                }
            }
        }, chatExecutor);

        return emitter;
    }

    /**
     * Get conversation history for a session
     */
    private List<Map<String, String>> getConversationHistory(String tenantId, String sessionId) {
        String key = buildKey(tenantId, sessionId);
        ChatContext context = sessionContexts.get(key);

        if (context == null) {
            return new ArrayList<>();
        }

        // Thread-safe access: refresh TTL and return a copy of messages
        return context.getMessagesCopy();
    }

    /**
     * Save user message and generate assistant response placeholder
     */
    private void saveToHistory(String tenantId, String sessionId, String userMessage, boolean enableThinking) {
        String key = buildKey(tenantId, sessionId);

        ChatContext context = sessionContexts.computeIfAbsent(key, k -> {
            ChatContext newContext = new ChatContext();
            newContext.setTenantId(tenantId);
            newContext.setSessionId(sessionId);
            newContext.setMessages(new ArrayList<>());
            newContext.setLastAccessTime(System.currentTimeMillis());
            return newContext;
        });

        // Thread-safe modification: add message and refresh TTL
        context.addMessageAndRefresh(Map.of("role", "user", "content", userMessage));
    }

    /**
     * Build session key
     */
    private String buildKey(String tenantId, String sessionId) {
        return tenantId + "_" + sessionId;
    }

    /**
     * Clean up expired sessions
     */
    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, ChatContext>> iterator = sessionContexts.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, ChatContext> entry = iterator.next();
            if (now - entry.getValue().getLastAccessTime() > sessionTtl) {
                iterator.remove();
                log.info("Cleaned up expired session: {}", entry.getKey());
            }
        }
    }

    /**
     * Create error emitter
     */
    private SseEmitter createErrorEmitter(String errorMessage) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event().data("data: {\"type\":\"error\",\"message\":\"" + errorMessage + "\"}"));
        } catch (IOException e) {
            log.warn("Failed to send error event", e);
        }
        emitter.complete();
        return emitter;
    }

    /**
     * Chat context for session management
     * Thread-safe: all access to mutable state is synchronized
     */
    private static class ChatContext {
        private String tenantId;
        private String sessionId;
        private List<Map<String, String>> messages = new ArrayList<>();
        private long lastAccessTime;

        public synchronized List<Map<String, String>> getMessagesCopy() {
            this.lastAccessTime = System.currentTimeMillis();
            return new ArrayList<>(messages);
        }

        public synchronized void addMessageAndRefresh(Map<String, String> message) {
            messages.add(message);
            this.lastAccessTime = System.currentTimeMillis();
            // Trim history if too long (keep last 20 messages)
            if (messages.size() > 20) {
                messages = new ArrayList<>(messages.subList(messages.size() - 20, messages.size()));
            }
        }

        public synchronized long getLastAccessTime() {
            return lastAccessTime;
        }

        // Setters for use by computeIfAbsent
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public void setMessages(List<Map<String, String>> messages) { this.messages = messages; }
        public void setLastAccessTime(long lastAccessTime) { this.lastAccessTime = lastAccessTime; }
    }
}
